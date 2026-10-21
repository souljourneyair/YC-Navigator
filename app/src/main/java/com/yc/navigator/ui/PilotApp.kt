package com.yc.navigator.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yc.navigator.BuildConfig
import com.yc.navigator.data.ThemeMode
import com.yc.navigator.data.UpdateManager
import com.yc.navigator.ui.theme.YCNavigatorTheme
import com.yc.navigator.R
import com.yc.navigator.export.ShareUtils
import com.yc.navigator.pdf.JournalPdfGenerator
import com.yc.navigator.ui.editor.JournalEditorScreen
import com.yc.navigator.ui.preview.PdfPreviewScreen
import com.yc.navigator.ui.screens.DocumentsScreen
import com.yc.navigator.ui.screens.HelpScreen
import com.yc.navigator.ui.screens.JournalsScreen
import com.yc.navigator.ui.screens.PilotsScreen
import com.yc.navigator.ui.screens.SettingsScreen
import com.yc.navigator.ui.screens.WeatherScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun PilotApp(viewModel: NavigatorViewModel = viewModel()) {
    val settings = viewModel.settings
    val languageTag = settings.languageTag
    val context = LocalContext.current
    val view = LocalView.current
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val importSuccessMessage = stringResource(R.string.msg_import_success)
    val importErrorMessage = stringResource(R.string.msg_import_error)
    val pdfActionTitle = stringResource(R.string.action_pdf)
    val pdfErrorMessage = stringResource(R.string.msg_pdf_error)
    val exportActionTitle = stringResource(R.string.settings_export_all)
    val exportReadyMessage = stringResource(R.string.msg_export_ready)
    val exportErrorMessage = stringResource(R.string.msg_export_error)
    val updateUpToDateMessage = stringResource(R.string.update_up_to_date)
    val updateDownloadingMessage = stringResource(R.string.update_downloading)
    val updateErrorMessage = stringResource(R.string.update_error)
    val updatePermissionMessage = stringResource(R.string.update_permission)
    val updateManager = remember { UpdateManager(context) }
    var updateInProgress by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val previousLanguage = viewModel.settings.languageTag
            runCatching {
                val raw = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                        ?: error("Cannot open import file")
                }
                viewModel.importJson(raw)
                previousLanguage != viewModel.settings.languageTag
            }.onSuccess { languageChanged ->
                if (languageChanged) {
                    Toast.makeText(
                        context,
                        importSuccessMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                    context.findActivity()?.recreate()
                } else {
                    snackbarHost.showSnackbar(importSuccessMessage)
                }
            }.onFailure {
                snackbarHost.showSnackbar(importErrorMessage)
            }
        }
    }

    val checkForUpdates: () -> Unit = {
        if (!updateInProgress) {
            updateInProgress = true
            scope.launch {
                try {
                    val info = updateManager.fetchLatest()
                    if (info.versionCode <= BuildConfig.VERSION_CODE) {
                        scope.launch { snackbarHost.showSnackbar(updateUpToDateMessage) }
                    } else if (!updateManager.canInstallPackages()) {
                        Toast.makeText(
                            context,
                            updatePermissionMessage,
                            Toast.LENGTH_LONG
                        ).show()
                        updateManager.openInstallPermissionSettings()
                    } else {
                        scope.launch { snackbarHost.showSnackbar(updateDownloadingMessage) }
                        val file = updateManager.download(info)
                        updateManager.install(file)
                    }
                } catch (_: Exception) {
                    scope.launch { snackbarHost.showSnackbar(updateErrorMessage) }
                } finally {
                    updateInProgress = false
                }
            }
        }
    }

    SideEffect {
        val activity = context.findActivity() ?: return@SideEffect
        WindowCompat.getInsetsController(activity.window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var editingJournalId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingJournal = editingJournalId?.let(viewModel::journalById)
    var previewJournalId by rememberSaveable { mutableStateOf<Long?>(null) }
    val previewJournal = previewJournalId?.let(viewModel::journalById)

    LaunchedEffect(editingJournalId, editingJournal) {
        if (editingJournalId != null && editingJournal == null) {
            editingJournalId = null
        }
    }

    LaunchedEffect(previewJournalId, previewJournal) {
        if (previewJournalId != null && previewJournal == null) {
            previewJournalId = null
        }
    }

    BackHandler(enabled = editingJournalId != null || previewJournalId != null) {
        if (previewJournalId != null) {
            previewJournalId = null
        } else {
            editingJournalId = null
        }
    }

    YCNavigatorTheme(darkTheme = darkTheme) {
    val navLabelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = {
                        tab = 0
                        editingJournalId = null
                        previewJournalId = null
                    },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = {
                        Text(
                            stringResource(R.string.nav_journals),
                            style = navLabelStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = {
                        tab = 1
                        editingJournalId = null
                        previewJournalId = null
                    },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = {
                        Text(
                            stringResource(R.string.nav_pilots),
                            style = navLabelStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = {
                        tab = 2
                        editingJournalId = null
                        previewJournalId = null
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.nav_documents),
                            style = navLabelStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = {
                        tab = 3
                        editingJournalId = null
                        previewJournalId = null
                    },
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_weather),
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.nav_weather),
                            style = navLabelStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                NavigationBarItem(
                    selected = tab == 4,
                    onClick = {
                        tab = 4
                        editingJournalId = null
                        previewJournalId = null
                    },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = {
                        Text(
                            stringResource(R.string.nav_help),
                            style = navLabelStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
                NavigationBarItem(
                    selected = tab == 5,
                    onClick = {
                        tab = 5
                        editingJournalId = null
                        previewJournalId = null
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = {
                        Text(
                            stringResource(R.string.nav_settings),
                            style = navLabelStyle,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (previewJournal != null) {
                PdfPreviewScreen(
                    journal = previewJournal,
                    pilots = viewModel.pilots,
                    settings = viewModel.settings,
                    onBack = { previewJournalId = null }
                )
            } else if (editingJournal != null) {
                key(editingJournal.id) {
                JournalEditorScreen(
                    journal = editingJournal,
                    pilots = viewModel.pilots,
                    settings = settings,
                    languageTag = languageTag,
                    aircraftTypes = viewModel.aircraftTypes,
                    onSaveAircraftType = viewModel::saveAircraftType,
                    onRemoveAircraftType = viewModel::removeAircraftType,
                    onJournalChange = viewModel::updateJournal,
                    onBack = { editingJournalId = null },
                    onDuplicate = { id ->
                        viewModel.duplicateJournal(id)?.let { copy ->
                            editingJournalId = copy.id
                        }
                    },
                    onDelete = viewModel::deleteJournal,
                    onPreview = { previewJournalId = editingJournal.id },
                    onSharePdf = { journal ->
                        scope.launch {
                            runCatching {
                                val file = withContext(Dispatchers.IO) {
                                    val number = journal.number.ifBlank { "log" }
                                    val output = ShareUtils.sharedFile(
                                        context,
                                        "YC-navigator_${number}_${journal.dateIso}.pdf"
                                    )
                                    JournalPdfGenerator.generate(
                                        context = context,
                                        journal = journal,
                                        pilots = viewModel.pilots,
                                        settings = viewModel.settings,
                                        outputFile = output
                                    )
                                }
                                ShareUtils.shareFile(
                                    context,
                                    file,
                                    "application/pdf",
                                    pdfActionTitle
                                )
                            }.onFailure {
                                snackbarHost.showSnackbar(pdfErrorMessage)
                            }
                        }
                    }
                )
                }
            } else {
                when (tab) {
                    0 -> JournalsScreen(
                        journals = viewModel.journals,
                        pilots = viewModel.pilots,
                        languageTag = languageTag,
                        onCreate = {
                            val journal = viewModel.createJournal()
                            editingJournalId = journal.id
                        },
                        onOpen = { editingJournalId = it },
                        onDuplicate = { id ->
                            viewModel.duplicateJournal(id)?.let { copy ->
                                editingJournalId = copy.id
                            }
                        },
                        onDelete = viewModel::deleteJournal
                    )

                    1 -> PilotsScreen(
                        pilots = viewModel.pilots,
                        journals = viewModel.journals,
                        onAdd = viewModel::addPilot,
                        onUpdate = viewModel::updatePilot,
                        onDelete = viewModel::deletePilot
                    )

                    2 -> DocumentsScreen(
                        documents = viewModel.documentItems,
                        snackbarHost = snackbarHost,
                        documentFile = viewModel::documentFile,
                        documentParentId = viewModel::documentParentId,
                        documentFolderDepth = viewModel::documentFolderDepth,
                        onAddFolder = viewModel::addDocumentFolder,
                        onRename = viewModel::renameDocumentNode,
                        onDelete = viewModel::deleteDocumentNode,
                        onImportDocument = viewModel::importDocument
                    )

                    3 -> WeatherScreen(
                        reports = viewModel.weatherReports,
                        languageTag = languageTag,
                        snackbarHost = snackbarHost,
                        onReportChange = viewModel::setWeatherReport,
                        onImportJson = viewModel::importWeatherReports,
                        onExportJson = viewModel::exportWeatherJson
                    )

                    4 -> HelpScreen()

                    else -> SettingsScreen(
                        settings = settings,
                        onLanguageChange = { tag ->
                            viewModel.setLanguage(tag)
                            context.findActivity()?.recreate()
                        },
                        onThemeModeChange = viewModel::setThemeMode,
                        onUnitModeChange = viewModel::setUnitMode,
                        onExportData = {
                            scope.launch {
                                runCatching {
                                    val json = viewModel.exportJson()
                                    val file = withContext(Dispatchers.IO) {
                                        val output = ShareUtils.sharedFile(
                                            context,
                                            "YC-navigator_data.json"
                                        )
                                        output.writeText(json, Charsets.UTF_8)
                                        output
                                    }
                                    ShareUtils.shareFile(
                                        context,
                                        file,
                                        "application/json",
                                        exportActionTitle
                                    )
                                    snackbarHost.showSnackbar(exportReadyMessage)
                                }.onFailure {
                                    snackbarHost.showSnackbar(exportErrorMessage)
                                }
                            }
                        },
                        onImportData = {
                            importLauncher.launch(arrayOf("*/*"))
                        },
                        onCheckForUpdates = checkForUpdates,
                        updateInProgress = updateInProgress
                    )
                }
            }
        }
    }
    }
}
