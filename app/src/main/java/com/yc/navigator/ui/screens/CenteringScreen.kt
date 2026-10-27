package com.yc.navigator.ui.screens

import android.graphics.Paint as AndroidPaint
import android.graphics.Typeface
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yc.navigator.R
import com.yc.navigator.domain.CenteringCategory
import com.yc.navigator.domain.CenteringInput
import com.yc.navigator.domain.CenteringResult
import com.yc.navigator.domain.calculateCentering
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CenteringScreen(
    languageTag: String,
    onBack: () -> Unit
) {
    val kgToLb = 2.2046226218
    var optional by rememberSaveable { mutableStateOf(false) }
    var categoryIndex by rememberSaveable { mutableIntStateOf(0) }
    var pilotsText by rememberSaveable { mutableStateOf("") }
    var pilotsLbs by rememberSaveable { mutableStateOf(true) }
    var rearText by rememberSaveable { mutableStateOf("") }
    var rearLbs by rememberSaveable { mutableStateOf(true) }
    var baggage1Text by rememberSaveable { mutableStateOf("") }
    var baggage1Lbs by rememberSaveable { mutableStateOf(true) }
    var baggage2Text by rememberSaveable { mutableStateOf("") }
    var baggage2Lbs by rememberSaveable { mutableStateOf(true) }
    var fuelText by rememberSaveable { mutableStateOf("") }
    var fuelLbs by rememberSaveable { mutableStateOf(true) }
    var allowanceText by rememberSaveable { mutableStateOf("") }
    var allowanceLbs by rememberSaveable { mutableStateOf(true) }

    val category = if (categoryIndex == 1) {
        CenteringCategory.UTILITY
    } else {
        CenteringCategory.NORMAL
    }

    fun toLb(text: String, isLbs: Boolean): Double =
        parseNumber(text)?.let { if (isLbs) it else it * kgToLb } ?: 0.0

    val pilotsLb = toLb(pilotsText, pilotsLbs)
    val rearLb = toLb(rearText, rearLbs)
    val baggage1Lb = toLb(baggage1Text, baggage1Lbs)
    val baggage2Lb = toLb(baggage2Text, baggage2Lbs)
    val fuelLb = toLb(fuelText, fuelLbs)
    val allowanceLb = toLb(allowanceText, allowanceLbs)

    val result = calculateCentering(
        CenteringInput(
            fuelWeightLb = fuelLb,
            pilotsWeightLb = pilotsLb,
            rearPassengersWeightLb = rearLb,
            baggage1WeightLb = baggage1Lb,
            baggage2WeightLb = baggage2Lb,
            fuelAllowanceLb = allowanceLb,
            category = category
        )
    )

    val weightFormat = remember(languageTag) {
        NumberFormat.getNumberInstance(localeFor(languageTag)).apply {
            maximumFractionDigits = 1
            minimumFractionDigits = 0
        }
    }
    val cgFormat = remember(languageTag) {
        NumberFormat.getNumberInstance(localeFor(languageTag)).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
    }

    fun displayWeightBoth(lb: Double): String {
        val lbText = weightFormat.format(lb)
        val kgText = weightFormat.format(lb / kgToLb)
        return "$lbText lb / $kgText kg"
    }

    fun convertText(text: String, fromLbs: Boolean, toLbs: Boolean): String {
        if (fromLbs == toLbs) return text
        val value = parseNumber(text) ?: return text
        val converted = if (toLbs) value * kgToLb else value / kgToLb
        return formatEditable(converted)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = "YC NAVIGATOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.centering_title),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val diagramWidth = if (maxWidth > 420.dp) 420.dp else maxWidth
                val diagramHeight = diagramWidth * (1024f / 768f)
                Box(
                    modifier = Modifier
                        .width(diagramWidth)
                        .aspectRatio(768f / 1024f)
                        .align(Alignment.TopCenter)
                ) {
                    Image(
                        painter = painterResource(R.drawable.centering_seating),
                        contentDescription = stringResource(R.string.centering_title),
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )

                    val x = if (optional) diagramWidth * 0.625f else diagramWidth * 0.225f
                    val fieldWidth = diagramWidth * 0.15f
                    OverlayWeightRow(
                        value = pilotsText,
                        isLbs = pilotsLbs,
                        onValueChange = { pilotsText = it },
                        onUnitChange = {
                            pilotsText = convertText(pilotsText, pilotsLbs, it)
                            pilotsLbs = it
                        },
                        modifier = Modifier
                            .offset(x = x, y = diagramHeight * 0.345f)
                            .width(fieldWidth)
                    )
                    OverlayWeightRow(
                        value = rearText,
                        isLbs = rearLbs,
                        onValueChange = { rearText = it },
                        onUnitChange = {
                            rearText = convertText(rearText, rearLbs, it)
                            rearLbs = it
                        },
                        modifier = Modifier
                            .offset(x = x, y = diagramHeight * 0.485f)
                            .width(fieldWidth)
                    )
                    OverlayWeightRow(
                        value = baggage1Text,
                        isLbs = baggage1Lbs,
                        onValueChange = { baggage1Text = it },
                        onUnitChange = {
                            baggage1Text = convertText(baggage1Text, baggage1Lbs, it)
                            baggage1Lbs = it
                        },
                        modifier = Modifier
                            .offset(x = x, y = diagramHeight * 0.615f)
                            .width(fieldWidth)
                    )
                    OverlayWeightRow(
                        value = baggage2Text,
                        isLbs = baggage2Lbs,
                        onValueChange = { baggage2Text = it },
                        onUnitChange = {
                            baggage2Text = convertText(baggage2Text, baggage2Lbs, it)
                            baggage2Lbs = it
                        },
                        modifier = Modifier
                            .offset(x = x, y = diagramHeight * 0.735f)
                            .width(fieldWidth)
                    )

                    val fraction = ((result.cgArmIn - 35.0) / (47.3 - 35.0)).coerceIn(0.0, 1.0)
                    val ballX = diagramWidth * (if (optional) 0.67f else 0.33f) +
                        if (optional) 20.dp else 0.dp
                    val ballY = diagramHeight * (0.28f + fraction.toFloat() * 0.20f)
                    CgSymbol(
                        modifier = Modifier
                            .offset(x = ballX - 10.dp, y = ballY)
                            .size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !optional,
                    onClick = { optional = false },
                    label = { Text(stringResource(R.string.centering_standard)) }
                )
                FilterChip(
                    selected = optional,
                    onClick = { optional = true },
                    label = { Text(stringResource(R.string.centering_optional)) }
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.centering_category),
                    style = MaterialTheme.typography.bodyMedium
                )
                FilterChip(
                    selected = category == CenteringCategory.NORMAL,
                    onClick = { categoryIndex = 0 },
                    label = { Text(stringResource(R.string.centering_normal)) }
                )
                FilterChip(
                    selected = category == CenteringCategory.UTILITY,
                    onClick = { categoryIndex = 1 },
                    label = { Text(stringResource(R.string.centering_utility)) }
                )
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = stringResource(R.string.centering_empty_constants),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    FullWeightRow(
                        label = stringResource(R.string.centering_fuel_gallons),
                        value = fuelText,
                        isLbs = fuelLbs,
                        onValueChange = { fuelText = it },
                        onUnitChange = {
                            fuelText = convertText(fuelText, fuelLbs, it)
                            fuelLbs = it
                        }
                    )
                    Text(
                        text = stringResource(R.string.centering_fuel_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    FullWeightRow(
                        label = stringResource(R.string.centering_fuel_allowance),
                        value = allowanceText,
                        isLbs = allowanceLbs,
                        onValueChange = { allowanceText = it },
                        onUnitChange = {
                            allowanceText = convertText(allowanceText, allowanceLbs, it)
                            allowanceLbs = it
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    ResultRow(
                        label = stringResource(R.string.centering_ramp_weight),
                        value = displayWeightBoth(result.rampWeightLb)
                    )
                    ResultRow(
                        label = stringResource(R.string.centering_ramp_moment),
                        value = weightFormat.format(result.rampMomentK) + " /1000"
                    )
                    ResultRow(
                        label = stringResource(R.string.centering_fuel_allowance),
                        value = if (allowanceLb > 0.0) {
                            "-${weightFormat.format(allowanceLb)} lb / " +
                                "-${weightFormat.format(allowanceLb / kgToLb)} kg / " +
                                "-${weightFormat.format(allowanceLb * 48.0 / 1000.0)}"
                        } else {
                            "0"
                        }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    ResultRow(
                        label = stringResource(R.string.centering_takeoff_weight),
                        value = displayWeightBoth(result.takeoffWeightLb),
                        emphasize = true
                    )
                    ResultRow(
                        label = stringResource(R.string.centering_takeoff_moment),
                        value = weightFormat.format(result.takeoffMomentK) + " /1000",
                        emphasize = true
                    )
                    ResultRow(
                        label = stringResource(R.string.centering_cg),
                        value = "${cgFormat.format(result.cgArmIn)} in"
                    )
                    ResultRow(
                        label = stringResource(R.string.centering_cg_limits),
                        value = stringResource(
                            R.string.centering_cg_limits_value,
                            cgFormat.format(result.forwardLimitIn),
                            cgFormat.format(result.aftLimitIn)
                        )
                    )

                    val issues = buildList {
                        if (!result.baggageOk) add(stringResource(R.string.centering_status_baggage))
                        if (!result.weightOk) add(stringResource(R.string.centering_status_overweight))
                        if (!result.cgOk) add(stringResource(R.string.centering_status_cg))
                    }
                    Spacer(Modifier.height(10.dp))
                    if (issues.isEmpty()) {
                        Text(
                            text = stringResource(R.string.centering_status_ok),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            issues.forEach { issue ->
                                Text(
                                    text = issue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.centering_check_note,
                    stringResource(
                        if (category == CenteringCategory.UTILITY) {
                            R.string.centering_utility
                        } else {
                            R.string.centering_normal
                        }
                    )
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.centering_charts),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CgSymbol(Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.centering_chart_point),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            val loadPoints = listOf(
                LoadPoint(
                    label = stringResource(R.string.centering_fuel_gallons),
                    weightLb = fuelLb,
                    momentK = fuelLb * 48.0 / 1000.0,
                    color = Color(0xFF1565C0)
                ),
                LoadPoint(
                    label = stringResource(R.string.centering_pilots),
                    weightLb = pilotsLb,
                    momentK = pilotsLb * 37.0 / 1000.0,
                    color = Color(0xFF2E7D32)
                ),
                LoadPoint(
                    label = stringResource(R.string.centering_rear),
                    weightLb = rearLb,
                    momentK = rearLb * 73.0 / 1000.0,
                    color = Color(0xFFEF6C00)
                ),
                LoadPoint(
                    label = stringResource(R.string.centering_baggage1),
                    weightLb = baggage1Lb,
                    momentK = baggage1Lb * 95.0 / 1000.0,
                    color = Color(0xFF6A1B9A)
                ),
                LoadPoint(
                    label = stringResource(R.string.centering_baggage2),
                    weightLb = baggage2Lb,
                    momentK = baggage2Lb * 123.0 / 1000.0,
                    color = Color(0xFF6D4C41)
                )
            )
            LoadingGraphChart(loadPoints)
            Spacer(Modifier.height(18.dp))
            EnvelopeChart(result)
            Spacer(Modifier.height(18.dp))
            LimitsChart(result)
            Spacer(Modifier.height(96.dp))
        }
    }
}

private data class LoadPoint(
    val label: String,
    val weightLb: Double,
    val momentK: Double,
    val color: Color
)

private data class ChartTransform(
    val left: Float,
    val top: Float,
    val plotWidth: Float,
    val plotHeight: Float,
    val xMin: Double,
    val xMax: Double,
    val yMin: Double,
    val yMax: Double
) {
    fun x(value: Double): Float =
        (left + ((value - xMin) / (xMax - xMin) * plotWidth)).toFloat()

    fun y(value: Double): Float =
        (top + ((yMax - value) / (yMax - yMin) * plotHeight)).toFloat()
}

private val ChartAxis = Color(0xFF424242)
private val ChartGrid = Color(0xFFE0E0E0)
private val ChartNormal = Color(0xFF1565C0)
private val ChartUtility = Color(0xFF757575)

@Composable
private fun LoadingGraphChart(loads: List<LoadPoint>) {
    ChartFrame(stringResource(R.string.centering_loading_graph)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
        ) {
            val t = drawChartBase(
                xMin = 0.0,
                xMax = 30.0,
                yMin = 0.0,
                yMax = 450.0,
                xGridStep = 5.0,
                xLabelStep = 5.0,
                yGridStep = 50.0,
                yLabelStep = 50.0,
                xTitle = "Moment / 1000, lb·in",
                yTitle = "Load weight, lb"
            )
            val fuelMax = 300.0
            val pilotsMax = 400.0
            val rearMax = 400.0
            val bag1Max = 120.0
            val bag2Max = 50.0
            drawLine(
                Color(0xFF1565C0),
                Offset(t.x(0.0), t.y(0.0)),
                Offset(t.x(fuelMax * 48.0 / 1000.0), t.y(fuelMax)),
                strokeWidth = 1.4.dp.toPx()
            )
            drawLine(
                Color(0xFF2E7D32),
                Offset(t.x(0.0), t.y(0.0)),
                Offset(t.x(pilotsMax * 37.0 / 1000.0), t.y(pilotsMax)),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                Color(0xFFEF6C00),
                Offset(t.x(0.0), t.y(0.0)),
                Offset(t.x(rearMax * 73.0 / 1000.0), t.y(rearMax)),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                Color(0xFF6A1B9A),
                Offset(t.x(0.0), t.y(0.0)),
                Offset(t.x(bag1Max * 95.0 / 1000.0), t.y(bag1Max)),
                strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
                Color(0xFF6D4C41),
                Offset(t.x(0.0), t.y(0.0)),
                Offset(t.x(bag2Max * 123.0 / 1000.0), t.y(bag2Max)),
                strokeWidth = 1.2.dp.toPx()
            )
            loads.filter { it.weightLb > 0.0 }.forEach { load ->
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(t.x(load.momentK), t.y(load.weightLb))
                )
                drawCircle(
                    color = load.color,
                    radius = 2.5.dp.toPx(),
                    center = Offset(t.x(load.momentK), t.y(load.weightLb))
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            loads.forEach { load ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(load.color, CircleShape)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = load.label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun EnvelopeChart(result: CenteringResult) {
    ChartFrame(stringResource(R.string.centering_cg_moment_envelope)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.45f)
        ) {
            val t = drawChartBase(
                xMin = 45.0,
                xMax = 110.0,
                yMin = 1500.0,
                yMax = 2300.0,
                xGridStep = 5.0,
                xLabelStep = 10.0,
                yGridStep = 100.0,
                yLabelStep = 100.0,
                xTitle = "Moment / 1000, lb·in",
                yTitle = "Loaded weight, lb"
            )
            val utility = chartPath(
                t,
                listOf(
                    1500.0 to 52.5,
                    2000.0 to 70.0,
                    2000.0 to 81.0,
                    1500.0 to 60.75
                )
            )
            val normal = chartPath(
                t,
                listOf(
                    1500.0 to 52.5,
                    1950.0 to 68.25,
                    2300.0 to 88.55,
                    2300.0 to 108.79,
                    1500.0 to 70.95
                )
            )
            drawPath(normal, ChartNormal.copy(alpha = 0.07f))
            drawPath(utility, ChartUtility.copy(alpha = 0.08f))
            drawPath(normal, ChartNormal, style = Stroke(width = 1.5.dp.toPx()))
            drawPath(
                utility,
                ChartUtility,
                style = Stroke(
                    width = 1.3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                )
            )
            chartLabel("NORMAL", t.x(97.0), t.y(2180.0), 9f, ChartNormal)
            chartLabel("UTILITY", t.x(67.0), t.y(1810.0), 8f, ChartUtility)
            drawCgSymbol(
                Offset(
                    t.x(result.takeoffMomentK).coerceIn(
                        t.left + 2.dp.toPx(),
                        t.left + t.plotWidth - 2.dp.toPx()
                    ),
                    t.y(result.takeoffWeightLb).coerceIn(
                        t.top + 2.dp.toPx(),
                        t.top + t.plotHeight - 2.dp.toPx()
                    )
                ),
                9.dp.toPx()
            )
        }
    }
}

@Composable
private fun LimitsChart(result: CenteringResult) {
    ChartFrame(stringResource(R.string.centering_cg_limits_chart)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.4f)
        ) {
            val t = drawChartBase(
                xMin = 34.0,
                xMax = 47.5,
                yMin = 1500.0,
                yMax = 2300.0,
                xGridStep = 1.0,
                xLabelStep = 2.0,
                yGridStep = 100.0,
                yLabelStep = 100.0,
                xTitle = "CG location, inches aft of datum",
                yTitle = "Loaded weight, lb"
            )
            val normal = chartPath(
                t,
                listOf(
                    1500.0 to 35.0,
                    1950.0 to 35.0,
                    2300.0 to 38.5,
                    2300.0 to 47.3,
                    1500.0 to 47.3,
                    1500.0 to 35.0
                )
            )
            val utility = chartPath(
                t,
                listOf(
                    1500.0 to 35.0,
                    2000.0 to 35.0,
                    2000.0 to 40.5,
                    1500.0 to 40.5,
                    1500.0 to 35.0
                )
            )
            drawPath(normal, ChartNormal.copy(alpha = 0.06f))
            drawPath(utility, ChartUtility.copy(alpha = 0.07f))
            drawPath(normal, ChartNormal, style = Stroke(width = 1.5.dp.toPx()))
            drawPath(
                utility,
                ChartUtility,
                style = Stroke(
                    width = 1.3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))
                )
            )
            chartLabel("NORMAL", t.x(44.0), t.y(1700.0), 9f, ChartNormal)
            chartLabel("UTILITY", t.x(37.0), t.y(1700.0), 8f, ChartUtility)
            drawCgSymbol(
                Offset(
                    t.x(result.cgArmIn).coerceIn(
                        t.left + 2.dp.toPx(),
                        t.left + t.plotWidth - 2.dp.toPx()
                    ),
                    t.y(result.takeoffWeightLb).coerceIn(
                        t.top + 2.dp.toPx(),
                        t.top + t.plotHeight - 2.dp.toPx()
                    )
                ),
                9.dp.toPx()
            )
        }
    }
}

@Composable
private fun ChartFrame(
    title: String,
    content: @Composable () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(8.dp)) {
                content()
            }
        }
    }
}

private fun DrawScope.drawChartBase(
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
    xGridStep: Double,
    xLabelStep: Double,
    yGridStep: Double,
    yLabelStep: Double,
    xTitle: String,
    yTitle: String
): ChartTransform {
    val left = 44.dp.toPx()
    val top = 18.dp.toPx()
    val right = 10.dp.toPx()
    val bottom = 28.dp.toPx()
    val plotWidth = size.width - left - right
    val plotHeight = size.height - top - bottom
    drawRect(Color.White, Offset.Zero, Size(size.width, size.height))

    var x = xMin
    while (x <= xMax + 0.001) {
        val px = (left + ((x - xMin) / (xMax - xMin) * plotWidth)).toFloat()
        drawLine(ChartGrid, Offset(px, top), Offset(px, top + plotHeight), 0.6.dp.toPx())
        x += xGridStep
    }
    var y = yMin
    while (y <= yMax + 0.001) {
        val py = (top + ((yMax - y) / (yMax - yMin) * plotHeight)).toFloat()
        drawLine(ChartGrid, Offset(left, py), Offset(left + plotWidth, py), 0.6.dp.toPx())
        y += yGridStep
    }
    drawLine(ChartAxis, Offset(left, top), Offset(left, top + plotHeight), 1.dp.toPx())
    drawLine(
        ChartAxis,
        Offset(left, top + plotHeight),
        Offset(left + plotWidth, top + plotHeight),
        1.dp.toPx()
    )

    var labelX = xMin
    while (labelX <= xMax + 0.001) {
        val px = (left + ((labelX - xMin) / (xMax - xMin) * plotWidth)).toFloat()
        chartLabel(labelX.roundToInt().toString(), px, top + plotHeight + 11.dp.toPx(), 7.5f)
        labelX += xLabelStep
    }
    var labelY = yMin
    while (labelY <= yMax + 0.001) {
        val py = (top + ((yMax - labelY) / (yMax - yMin) * plotHeight)).toFloat()
        chartLabel(
            labelY.roundToInt().toString(),
            left - 4.dp.toPx(),
            py + 2.dp.toPx(),
            sizeSp = 7.5f,
            align = AndroidPaint.Align.RIGHT
        )
        labelY += yLabelStep
    }
    chartLabel(
        xTitle,
        left + plotWidth / 2f,
        size.height - 3.dp.toPx(),
        sizeSp = 8f
    )
    chartLabel(
        yTitle,
        left + plotWidth / 2f,
        8.dp.toPx(),
        sizeSp = 8f
    )
    return ChartTransform(left, top, plotWidth, plotHeight, xMin, xMax, yMin, yMax)
}

private fun DrawScope.chartLabel(
    text: String,
    x: Float,
    y: Float,
    sizeSp: Float,
    color: Color = ChartAxis,
    align: AndroidPaint.Align = AndroidPaint.Align.CENTER
) {
    val paint = AndroidPaint().apply {
        this.color = color.toArgb()
        textSize = sizeSp.sp.toPx()
        this.textAlign = align
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(text, x, y, paint)
}

private fun chartPath(
    t: ChartTransform,
    points: List<Pair<Double, Double>>
): Path = Path().apply {
    points.forEachIndexed { index, point ->
        val x = t.x(point.second)
        val y = t.y(point.first)
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
private fun CgSymbol(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = size.minDimension / 2f - 1.dp.toPx()
        drawCgSymbol(center, radius)
    }
}

private fun DrawScope.drawCgSymbol(center: Offset, radius: Float) {
    drawCircle(Color.White, radius + 1.5.dp.toPx(), center)
    drawCircle(Color.White, radius, center)
    val topLeft = Offset(center.x - radius, center.y - radius)
    val circleSize = Size(radius * 2f, radius * 2f)
    drawArc(
        color = Color.Black,
        startAngle = 180f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = topLeft,
        size = circleSize
    )
    drawArc(
        color = Color.Black,
        startAngle = 0f,
        sweepAngle = 90f,
        useCenter = true,
        topLeft = topLeft,
        size = circleSize
    )
    drawLine(
        color = Color.Black,
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = Color.Black,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = 1.dp.toPx()
    )
    drawCircle(
        color = Color.Black,
        radius = radius,
        center = center,
        style = Stroke(width = 1.2.dp.toPx())
    )
}

@Composable
private fun OverlayWeightRow(
    value: String,
    isLbs: Boolean,
    onValueChange: (String) -> Unit,
    onUnitChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompactNumberField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(2.dp))
        LbsCheckbox(checked = isLbs, onCheckedChange = onUnitChange)
    }
}

@Composable
private fun CompactNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .height(28.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                RoundedCornerShape(4.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 3.dp, vertical = 5.dp),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center) {
                if (value.isBlank()) {
                    Text(
                        text = "—",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun FullWeightRow(
    label: String,
    value: String,
    isLbs: Boolean,
    onValueChange: (String) -> Unit,
    onUnitChange: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        LbsCheckbox(checked = isLbs, onCheckedChange = onUnitChange)
    }
}

@Composable
private fun LbsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(2.dp)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(2.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Canvas(Modifier.size(9.dp)) {
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width * 0.12f, size.height * 0.52f),
                        end = Offset(size.width * 0.42f, size.height * 0.82f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(size.width * 0.42f, size.height * 0.82f),
                        end = Offset(size.width * 0.9f, size.height * 0.15f),
                        strokeWidth = 1.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        Spacer(Modifier.width(1.dp))
        Text(
            text = "lbs",
            fontSize = 7.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    emphasize: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = if (emphasize) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.End
        )
    }
}

private fun localeFor(languageTag: String): Locale =
    Locale.forLanguageTag(if (languageTag == "en") "en" else "ru-RU")

private fun parseNumber(text: String): Double? =
    text.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }

private fun formatEditable(value: Double): String =
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
