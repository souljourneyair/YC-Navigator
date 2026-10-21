package com.yc.navigator.ui.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yc.navigator.R
import com.yc.navigator.data.AppSettings
import com.yc.navigator.data.Journal
import com.yc.navigator.data.Pilot
import com.yc.navigator.export.ShareUtils
import com.yc.navigator.pdf.JournalPdfGenerator
import com.yc.navigator.ui.components.PdfPagesViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    journal: Journal,
    pilots: List<Pilot>,
    settings: AppSettings,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val shareLabel = stringResource(R.string.action_share)
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var attempt by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(journal.id, attempt) {
        loading = true
        failed = false
        runCatching {
            withContext(Dispatchers.IO) {
                val output = ShareUtils.sharedFile(context, "preview_${journal.id}.pdf")
                JournalPdfGenerator.generate(
                    context = context,
                    journal = journal,
                    pilots = pilots,
                    settings = settings,
                    outputFile = output
                )
                output
            }
        }.onSuccess { file ->
            pdfFile = file
            loading = false
        }.onFailure {
            failed = true
            loading = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preview_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    pdfFile?.let { file ->
                        IconButton(onClick = {
                            ShareUtils.shareFile(
                                context,
                                file,
                                "application/pdf",
                                shareLabel
                            )
                        }) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = stringResource(R.string.action_share)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        when {
            loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.preview_loading))
                }
            }

            failed -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.preview_error))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { attempt += 1 }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            else -> pdfFile?.let { file ->
                PdfPagesViewer(
                    file = file,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}
