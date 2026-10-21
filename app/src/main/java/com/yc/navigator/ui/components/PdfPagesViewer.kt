package com.yc.navigator.ui.components

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yc.navigator.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

private const val BASE_RENDER_SCALE = 2f
private const val MAX_RENDER_SCALE = 6f
private const val MAX_ZOOM = 4f
private const val RENDER_DEBOUNCE_MS = 300L
private const val ZOOM_CONTROLS_HIDE_MS = 3000L

@Composable
fun PdfPagesViewer(
    file: File,
    modifier: Modifier = Modifier
) {
    var pages by remember(file.path) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var loading by remember(file.path) { mutableStateOf(true) }
    var failed by remember(file.path) { mutableStateOf(false) }
    var attempt by remember(file.path) { mutableIntStateOf(0) }
    var zoom by remember(file.path) { mutableStateOf(1f) }
    var renderScale by remember(file.path) { mutableStateOf(BASE_RENDER_SCALE) }
    var zoomControlsVisible by remember(file.path) { mutableStateOf(true) }
    var zoomControlsTick by remember(file.path) { mutableIntStateOf(0) }
    val renderMutex = remember(file.path) { Mutex() }

    fun showZoomControls() {
        zoomControlsVisible = true
        zoomControlsTick += 1
    }

    LaunchedEffect(file.path, zoomControlsVisible, zoomControlsTick) {
        if (zoomControlsVisible) {
            delay(ZOOM_CONTROLS_HIDE_MS)
            zoomControlsVisible = false
        }
    }

    LaunchedEffect(file.path, attempt) {
        loading = true
        failed = false
        runCatching {
            withContext(Dispatchers.IO) { renderPdf(file, BASE_RENDER_SCALE) }
        }.onSuccess { bitmaps ->
            pages = bitmaps
            renderScale = BASE_RENDER_SCALE
            loading = false
        }.onFailure {
            failed = true
            loading = false
        }
    }

    LaunchedEffect(file.path, zoom) {
        delay(RENDER_DEBOUNCE_MS)
        val target = (BASE_RENDER_SCALE * zoom).coerceAtMost(MAX_RENDER_SCALE)
        if (target <= renderScale) return@LaunchedEffect
        renderMutex.withLock {
            if (target <= renderScale) return@withLock
            runCatching {
                withContext(Dispatchers.IO) { renderPdf(file, target) }
            }.onSuccess { bitmaps ->
                pages = bitmaps
                renderScale = target
            }
        }
    }

    DisposableEffect(file.path) {
        onDispose {
            pages.forEach { bitmap ->
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }
    }

    when {
        loading -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.pdf_loading))
            }
        }

        failed -> Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.pdf_open_error))
                Spacer(Modifier.height(12.dp))
                Button(onClick = { attempt += 1 }) {
                    Text(stringResource(R.string.action_retry))
                }
            }
        }

        else -> BoxWithConstraints(modifier = modifier) {
            val viewportWidth = maxWidth
            val horizontalScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        showZoomControls()
                    }
                    .horizontalScroll(horizontalScroll)
                    .pinchToZoom { factor ->
                        zoom = (zoom * factor).coerceIn(1f, MAX_ZOOM)
                        showZoomControls()
                    }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .width(viewportWidth * zoom)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(pages) { _, bitmap ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(
                                        bitmap.width.toFloat() / bitmap.height.toFloat()
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = zoomControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                ZoomBar(
                    zoom = zoom,
                    onZoomOut = {
                        zoom = (zoom - 0.5f).coerceAtLeast(1f)
                        showZoomControls()
                    },
                    onZoomIn = {
                        zoom = (zoom + 0.5f).coerceAtMost(MAX_ZOOM)
                        showZoomControls()
                    },
                    onReset = {
                        zoom = 1f
                        showZoomControls()
                    }
                )
            }
        }
    }
}

@Composable
private fun ZoomBar(
    modifier: Modifier = Modifier,
    zoom: Float,
    onZoomOut: () -> Unit,
    onZoomIn: () -> Unit,
    onReset: () -> Unit
) {
    val zoomOutDescription = stringResource(R.string.action_zoom_out)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            IconButton(
                onClick = onZoomOut,
                enabled = zoom > 1f
            ) {
                Text(
                    text = "−",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics {
                        contentDescription = zoomOutDescription
                    }
                )
            }
            TextButton(onClick = onReset) {
                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center
                )
            }
            IconButton(
                onClick = onZoomIn,
                enabled = zoom < MAX_ZOOM
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.action_zoom_in)
                )
            }
        }
    }
}

private fun Modifier.pinchToZoom(onZoom: (Float) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var totalZoom = 1f
        var pastTouchSlop = false
        do {
            val event = awaitPointerEvent()
            if (event.changes.count { it.pressed } < 2) continue
            val zoomChange = event.calculateZoom()
            totalZoom *= zoomChange
            if (!pastTouchSlop) {
                val zoomMotion = abs(1f - totalZoom) *
                    event.calculateCentroidSize(useCurrent = false)
                pastTouchSlop = zoomMotion > viewConfiguration.touchSlop
            }
            if (pastTouchSlop && zoomChange != 1f) {
                onZoom(zoomChange)
                event.changes.forEach { change ->
                    if (change.pressed) change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun renderPdf(file: File, scale: Float): List<Bitmap> {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    try {
        val renderer = PdfRenderer(descriptor)
        try {
            val result = ArrayList<Bitmap>(renderer.pageCount)
            for (index in 0 until renderer.pageCount) {
                val page = renderer.openPage(index)
                try {
                    val bitmap = Bitmap.createBitmap(
                        (page.width * scale).toInt(),
                        (page.height * scale).toInt(),
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    result += bitmap
                } finally {
                    page.close()
                }
            }
            return result
        } finally {
            renderer.close()
        }
    } finally {
        descriptor.close()
    }
}
