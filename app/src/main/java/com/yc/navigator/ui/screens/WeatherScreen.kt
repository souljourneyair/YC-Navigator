package com.yc.navigator.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yc.navigator.R
import com.yc.navigator.data.WeatherNetworkClient
import com.yc.navigator.data.WeatherReportType
import com.yc.navigator.domain.WeatherDecodedSection
import com.yc.navigator.domain.decodeWeatherSections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class WeatherFileFormat {
    TXT,
    JSON
}

@Composable
fun WeatherScreen(
    reports: Map<String, String>,
    languageTag: String,
    snackbarHost: SnackbarHostState,
    onReportChange: (String, String) -> Unit,
    onImportJson: (String) -> Boolean,
    onExportJson: () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val importSuccess = stringResource(R.string.weather_import_success)
    val importError = stringResource(R.string.weather_import_error)
    val exportSuccess = stringResource(R.string.weather_export_success)
    val exportError = stringResource(R.string.weather_export_error)
    val loadSuccess = stringResource(R.string.weather_load_success)
    val loadError = stringResource(R.string.weather_load_error)
    val loadEmpty = stringResource(R.string.weather_load_empty)

    var selectedTypeKey by rememberSaveable {
        mutableStateOf(WeatherReportType.METAR_TAF.key)
    }
    val selectedType = WeatherReportType.fromKey(selectedTypeKey)
        ?: WeatherReportType.METAR_TAF
    val currentText = reports[selectedType.key].orEmpty()
    val decodedSections = remember(selectedType.key, currentText, languageTag) {
        decodeWeatherSections(selectedType.key, currentText, languageTag)
    }

    var pendingImportFormat by remember { mutableStateOf<WeatherFileFormat?>(null) }
    var pendingExportText by remember { mutableStateOf<String?>(null) }
    var showLoadDialog by remember { mutableStateOf(false) }
    var stationInput by rememberSaveable { mutableStateOf("") }
    var loadingWeather by remember { mutableStateOf(false) }

    fun loadFromNetwork() {
        val station = stationInput.trim().uppercase()
        if (station.isBlank()) return
        showLoadDialog = false
        loadingWeather = true
        scope.launch {
            val result = runCatching {
                WeatherNetworkClient.fetchMetarTaf(station)
            }
            loadingWeather = false
            result.onSuccess { code ->
                if (code.isNullOrBlank()) {
                    snackbarHost.showSnackbar(loadEmpty)
                } else {
                    onReportChange(WeatherReportType.METAR_TAF.key, code)
                    snackbarHost.showSnackbar(loadSuccess)
                }
            }.onFailure {
                snackbarHost.showSnackbar(loadError)
            }
        }
    }

    fun openLoadDialog() {
        if (stationInput.isBlank()) {
            stationInput = Regex("\\b[A-Z]{4}\\b")
                .find(currentText.uppercase())
                ?.value
                .orEmpty()
        }
        showLoadDialog = true
    }

    fun writeExport(uri: Uri, text: String) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(text)
                    } ?: error("Cannot open output")
                }.isSuccess
            }
            snackbarHost.showSnackbar(if (ok) exportSuccess else exportError)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val format = pendingImportFormat
        pendingImportFormat = null
        if (uri != null && format != null) {
            scope.launch {
                val text = withContext(Dispatchers.IO) {
                    readTextFile(context, uri)
                }
                if (text == null) {
                    snackbarHost.showSnackbar(importError)
                } else {
                    when (format) {
                        WeatherFileFormat.TXT -> {
                            onReportChange(selectedType.key, text)
                            snackbarHost.showSnackbar(importSuccess)
                        }
                        WeatherFileFormat.JSON -> {
                            snackbarHost.showSnackbar(
                                if (onImportJson(text)) importSuccess else importError
                            )
                        }
                    }
                }
            }
        }
    }

    val exportTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        val text = pendingExportText
        pendingExportText = null
        if (uri != null && text != null) {
            writeExport(uri, text)
        }
    }
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val text = pendingExportText
        pendingExportText = null
        if (uri != null && text != null) {
            writeExport(uri, text)
        }
    }

    fun startImport(format: WeatherFileFormat) {
        pendingImportFormat = format
        importLauncher.launch(arrayOf("*/*"))
    }

    fun startExport(format: WeatherFileFormat) {
        val text = when (format) {
            WeatherFileFormat.TXT -> currentText
            WeatherFileFormat.JSON -> onExportJson()
        }
        pendingExportText = text
        when (format) {
            WeatherFileFormat.TXT ->
                exportTxtLauncher.launch("weather_${selectedType.key}.txt")
            WeatherFileFormat.JSON ->
                exportJsonLauncher.launch("weather.json")
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 760.dp
        val drawerState = rememberDrawerState(DrawerValue.Closed)

        if (compact) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet {
                        WeatherSidebar(
                            selectedType = selectedType,
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight(),
                            onSelect = { type ->
                                selectedTypeKey = type.key
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                WeatherContent(
                    selectedType = selectedType,
                    text = currentText,
                    decoded = decodedSections,
                    compact = true,
                    isLoadingWeather = loadingWeather,
                    onOpenReports = { scope.launch { drawerState.open() } },
                    onLoadFromNetwork = ::openLoadDialog,
                    onTextChange = { onReportChange(selectedType.key, it) },
                    onImport = ::startImport,
                    onExport = ::startExport,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                WeatherSidebar(
                    selectedType = selectedType,
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    onSelect = { selectedTypeKey = it.key }
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                WeatherContent(
                    selectedType = selectedType,
                    text = currentText,
                    decoded = decodedSections,
                    compact = false,
                    isLoadingWeather = loadingWeather,
                    onOpenReports = {},
                    onLoadFromNetwork = ::openLoadDialog,
                    onTextChange = { onReportChange(selectedType.key, it) },
                    onImport = ::startImport,
                    onExport = ::startExport,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }

    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text(stringResource(R.string.weather_load_title)) },
            text = {
                OutlinedTextField(
                    value = stationInput,
                    onValueChange = { value ->
                        stationInput = value.uppercase()
                            .filter { it in 'A'..'Z' || it in '0'..'9' }
                            .take(6)
                    },
                    label = { Text(stringResource(R.string.weather_station)) },
                    placeholder = { Text(stringResource(R.string.weather_station_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = stationInput.isNotBlank() && !loadingWeather,
                    onClick = ::loadFromNetwork
                ) {
                    Text(stringResource(R.string.weather_load))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLoadDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun WeatherSidebar(
    selectedType: WeatherReportType,
    onSelect: (WeatherReportType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = stringResource(R.string.weather_reports),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            WeatherReportType.entries.forEach { type ->
                val selected = type == selectedType
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .clickable { onSelect(type) }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column {
                        Text(
                            text = type.key,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(weatherTypeDescription(type)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherContent(
    selectedType: WeatherReportType,
    text: String,
    decoded: List<WeatherDecodedSection>,
    compact: Boolean,
    isLoadingWeather: Boolean,
    onOpenReports: () -> Unit,
    onLoadFromNetwork: () -> Unit,
    onTextChange: (String) -> Unit,
    onImport: (WeatherFileFormat) -> Unit,
    onExport: (WeatherFileFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (compact) {
                IconButton(onClick = onOpenReports) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = stringResource(R.string.weather_open_reports)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = selectedType.key,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(weatherTypeDescription(selectedType)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!compact) {
                WeatherFileButtons(
                    onImport = onImport,
                    onExport = onExport
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (compact) {
                WeatherFileButtons(
                    onImport = onImport,
                    onExport = onExport
                )
                Spacer(Modifier.height(12.dp))
            }

            if (selectedType == WeatherReportType.METAR_TAF) {
                OutlinedButton(
                    onClick = onLoadFromNetwork,
                    enabled = !isLoadingWeather,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoadingWeather) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.weather_loading))
                    } else {
                        Text(stringResource(R.string.weather_load_network))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.weather_data_source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
            }

            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = {
                    Text(stringResource(R.string.weather_code_label, selectedType.key))
                },
                placeholder = { Text(stringResource(R.string.weather_code_hint)) },
                singleLine = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp)
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.weather_decoding),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            when {
                text.isBlank() -> Text(
                    text = stringResource(R.string.weather_decoding_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                decoded.isEmpty() -> Text(
                    text = stringResource(R.string.weather_unrecognized),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                else -> DecodedWeatherCard(decoded)
            }
        }
    }
}

@Composable
private fun WeatherFileButtons(
    onImport: (WeatherFileFormat) -> Unit,
    onExport: (WeatherFileFormat) -> Unit
) {
    var importMenu by remember { mutableStateOf(false) }
    var exportMenu by remember { mutableStateOf(false) }

    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        Box {
            OutlinedButton(onClick = { importMenu = true }) {
                Text(stringResource(R.string.weather_import))
            }
            DropdownMenu(
                expanded = importMenu,
                onDismissRequest = { importMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.weather_txt)) },
                    onClick = {
                        importMenu = false
                        onImport(WeatherFileFormat.TXT)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.weather_json)) },
                    onClick = {
                        importMenu = false
                        onImport(WeatherFileFormat.JSON)
                    }
                )
            }
        }
        Box {
            OutlinedButton(onClick = { exportMenu = true }) {
                Text(stringResource(R.string.weather_export))
            }
            DropdownMenu(
                expanded = exportMenu,
                onDismissRequest = { exportMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.weather_txt)) },
                    onClick = {
                        exportMenu = false
                        onExport(WeatherFileFormat.TXT)
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.weather_json)) },
                    onClick = {
                        exportMenu = false
                        onExport(WeatherFileFormat.JSON)
                    }
                )
            }
        }
    }
}

@Composable
private fun DecodedWeatherCard(sections: List<WeatherDecodedSection>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
        ) {
            sections.forEachIndexed { index, section ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                section.lines.forEach { line ->
                    Column {
                        Text(
                            text = line.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = line.value,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun weatherTypeDescription(type: WeatherReportType): Int = when (type) {
    WeatherReportType.METAR_TAF -> R.string.weather_type_metar_desc
    WeatherReportType.GAMET -> R.string.weather_type_gamet_desc
    WeatherReportType.SIGMET -> R.string.weather_type_sigmet_desc
    WeatherReportType.AIRMET -> R.string.weather_type_airmet_desc
}

private fun readTextFile(context: Context, uri: Uri): String? = runCatching {
    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
        it.readText()
    }
}.getOrNull()
