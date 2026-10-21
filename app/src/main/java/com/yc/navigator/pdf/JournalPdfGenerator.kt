package com.yc.navigator.pdf

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.yc.navigator.R
import com.yc.navigator.data.AppSettings
import com.yc.navigator.data.Journal
import com.yc.navigator.data.Leg
import com.yc.navigator.data.Pilot
import com.yc.navigator.data.Quantity
import com.yc.navigator.data.UnitConverter
import com.yc.navigator.data.UnitSystem
import com.yc.navigator.data.effectiveUnitSystem
import com.yc.navigator.domain.JournalCalculation
import com.yc.navigator.domain.JournalCalculator
import com.yc.navigator.ui.formatDate
import com.yc.navigator.ui.formatMinutes
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max


object JournalPdfGenerator {

    private const val PAGE_WIDTH = 842f
    private const val PAGE_HEIGHT = 595f
    private const val MARGIN = 12f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
    private const val TABLE_HEADER_HEIGHT = 20f
    private const val ROW_HEIGHT = 13f
    private const val BOTTOM_BAND_TOP = 494f

    fun generate(
        context: Context,
        journal: Journal,
        pilots: List<Pilot>,
        settings: AppSettings,
        outputFile: File
    ): File {
        val languageTag = settings.languageTag
        val locale = if (languageTag == "en") Locale.ENGLISH else Locale.forLanguageTag("ru-RU")
        val localized = localizedContext(context, locale)
        val units = settings.effectiveUnitSystem()
        val calc = JournalCalculator.calculate(journal)
        val pages = buildPages(journal, calc)

        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, rows ->
                val pageInfo = PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH.toInt(),
                    PAGE_HEIGHT.toInt(),
                    index + 1
                ).create()
                val page = document.startPage(pageInfo)
                val painter = Painter(page.canvas, localized, locale, units)
                drawPage(
                    painter = painter,
                    pageNumber = index + 1,
                    rows = rows,
                    journal = journal,
                    calc = calc,
                    pilots = pilots,
                    settings = settings,
                    units = units,
                    locale = locale,
                    languageTag = languageTag,
                    isLast = index == pages.lastIndex,
                    firstPage = index == 0
                )
                document.finishPage(page)
            }
            outputFile.parentFile?.mkdirs()
            outputFile.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
        return outputFile
    }

    private fun localizedContext(context: Context, locale: Locale): Context {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun buildPages(journal: Journal, calc: JournalCalculation): List<List<PdfRow>> {
        val legs = journal.legs
        val pages = mutableListOf<List<PdfRow>>()
        var cursor = 0

        if (legs.isEmpty()) {
            return listOf(
                listOf(
                    PdfRow.takeoff(journal, calc),
                    PdfRow.landing(journal, calc)
                )
            )
        }

        while (cursor < legs.size) {
            val capacity = if (pages.isEmpty()) 20 else 21
            val chunk = legs.drop(cursor).take(capacity)
            cursor += chunk.size
            val last = cursor >= legs.size
            val rows = mutableListOf<PdfRow>()
            if (pages.isEmpty()) rows += PdfRow.takeoff(journal, calc)
            chunk.forEachIndexed { index, leg ->
                val absoluteIndex = cursor - chunk.size + index
                rows += PdfRow.leg(leg, calc.legs.getOrNull(absoluteIndex), index + 1)
            }
            if (last) rows += PdfRow.landing(journal, calc)
            pages += rows
        }

        return pages
    }

    private fun drawPage(
        painter: Painter,
        pageNumber: Int,
        rows: List<PdfRow>,
        journal: Journal,
        calc: JournalCalculation,
        pilots: List<Pilot>,
        settings: AppSettings,
        units: UnitSystem,
        locale: Locale,
        languageTag: String,
        isLast: Boolean,
        firstPage: Boolean
    ) {
        val ctx = painter.context
        val tableTop = if (firstPage) {
            drawFullHeader(painter, journal, calc, pilots, units, locale, languageTag)
            172f
        } else {
            drawContinuationHeader(painter, journal, pageNumber, locale, languageTag)
            54f
        }

        drawRouteTable(
            painter = painter,
            rows = rows,
            calc = calc,
            journal = journal,
            top = tableTop,
            units = units,
            locale = locale,
            languageTag = languageTag
        )

        val totalsTop = tableTop + TABLE_HEADER_HEIGHT + rows.size * ROW_HEIGHT
        if (isLast) {
            drawTotals(painter, calc, totalsTop, units, locale, languageTag)
            drawBottomBand(painter, journal, pilots, units, locale, languageTag)
        }

        painter.text(
            text = ctx.getString(R.string.pdf_generated),
            x = MARGIN,
            y = PAGE_HEIGHT - 6f,
            size = 5f,
            color = Color.DKGRAY
        )
        painter.text(
            text = ctx.getString(R.string.pdf_page, pageNumber),
            x = PAGE_WIDTH - MARGIN,
            y = PAGE_HEIGHT - 6f,
            size = 5f,
            color = Color.DKGRAY,
            align = Paint.Align.RIGHT
        )
    }

    private fun drawFullHeader(
        painter: Painter,
        journal: Journal,
        calc: JournalCalculation,
        pilots: List<Pilot>,
        units: UnitSystem,
        locale: Locale,
        languageTag: String
    ): Float {
        val ctx = painter.context

        // Wind table
        val windX = MARGIN
        val windY = MARGIN
        val wCols = floatArrayOf(68f, 38f, 47f, 47f)
        val windHeader = if (languageTag == "en") "Points" else "Пункты"
        painter.cell(windX, windY, wCols[0], 15f, windHeader, 5.0f, true, Paint.Align.CENTER, true)
        var x = windX + wCols[0]
        painter.cell(x, windY, wCols[1], 15f, ctx.getString(R.string.field_height), 5.2f, true, Paint.Align.CENTER, true)
        x += wCols[1]
        painter.cell(x, windY, wCols[2], 15f, "δ", 5.4f, true, Paint.Align.CENTER, true)
        x += wCols[2]
        painter.cell(x, windY, wCols[3], 15f, "U", 5.4f, true, Paint.Align.CENTER, true)

        val wind = journal.windWeather
        val rowY = windY + 15f
        painter.cell(windX, rowY, wCols[0], 15f, wind.point, 6.5f, false, Paint.Align.LEFT)
        painter.cell(
            windX + wCols[0], rowY, wCols[1], 15f,
            num(wind.heightM, Quantity.ALTITUDE, units, locale, 0),
            6.5f, false, Paint.Align.CENTER
        )
        val windDirX = windX + wCols[0] + wCols[1]
        painter.cell(
            windDirX, rowY, wCols[2], 15f,
            num(wind.windDirectionTrue, Quantity.NONE, units, locale, 0),
            6.5f, false, Paint.Align.CENTER
        )
        val windSpeedX = windDirX + wCols[2]
        painter.cell(
            windSpeedX, rowY, wCols[3], 15f,
            num(wind.windSpeedKmh, Quantity.SPEED, units, locale, 0),
            6.5f, false, Paint.Align.CENTER
        )

        val secondY = rowY + 15f
        painter.cell(windX, secondY, wCols[0], 15f, "", 5.2f, false, Paint.Align.CENTER)
        painter.cell(windX + wCols[0], secondY, wCols[1], 15f, "∆M", 5.4f, false, Paint.Align.CENTER)
        painter.cell(
            windX + wCols[0] + wCols[1], secondY, wCols[2], 15f,
            num(wind.magneticDeclination, Quantity.NONE, units, locale, 1),
            6.5f, false, Paint.Align.CENTER
        )
        painter.cell(
            windX + wCols[0] + wCols[1] + wCols[2], secondY, wCols[3], 15f,
            "", 5.2f, false, Paint.Align.CENTER
        )

        val thirdY = secondY + 15f
        painter.cell(windX, thirdY, wCols[0], 15f, "", 5.2f, false, Paint.Align.CENTER)
        painter.cellSubscript(
            x = windX + wCols[0],
            y = thirdY,
            w = wCols[1],
            h = 15f,
            main = "δ",
            sub = "н",
            bold = true
        )
        painter.cell(
            windX + wCols[0] + wCols[1], thirdY, wCols[2], 15f,
            degrees(calc.windDirectionMagneticDeg, locale),
            6.5f, true, Paint.Align.CENTER
        )
        painter.cell(
            windX + wCols[0] + wCols[1] + wCols[2], thirdY, wCols[3], 15f,
            "", 5.2f, false, Paint.Align.CENTER
        )

        // Title and header fields
        val titleX = 220f
        val titleY = MARGIN
        val titleW = 360f
        val title = ctx.getString(R.string.pdf_title) + " " +
            (if (languageTag == "en") "No." else "№") + " " +
            journal.number.ifBlank { "____" }
        painter.text(title, titleX + titleW / 2f, titleY + 15f, 10.5f, true, Color.BLACK, Paint.Align.CENTER, titleW)

        val commander = pilotName(journal.pilots.commanderId, pilots)
        val second = pilotName(journal.pilots.secondPilotId, pilots)
        val commanderLabel = if (languageTag == "en") "Cdr" else "КВС"
        val secondLabel = if (languageTag == "en") "2nd pilot" else "2-й пилот"
        val dateLabel = if (languageTag == "en") "Date" else "Дата"
        val unitLabel = if (languageTag == "en") "Unit" else "Подразделение"
        val typeLabel = if (languageTag == "en") "Type" else "Тип ВС"
        val numberLabel = if (languageTag == "en") "No." else "№"

        painter.inlineField(
            commanderLabel,
            commander,
            titleX,
            titleY + 26f,
            22f,
            112f
        )
        painter.inlineField(
            secondLabel,
            second,
            titleX + 114f,
            titleY + 26f,
            48f,
            150f
        )
        painter.inlineField(
            dateLabel,
            formatDate(journal.dateIso, languageTag),
            titleX + 266f,
            titleY + 26f,
            22f,
            94f
        )
        painter.inlineField(
            unitLabel,
            journal.unit,
            titleX,
            titleY + 40f,
            if (languageTag == "en") 28f else 58f,
            360f
        )
        painter.inlineField(
            typeLabel,
            journal.aircraftType,
            titleX,
            titleY + 54f,
            if (languageTag == "en") 28f else 38f,
            220f
        )
        painter.inlineField(
            numberLabel,
            journal.aircraftNumber,
            titleX + 224f,
            titleY + 54f,
            14f,
            136f
        )

        // Sun table
        val sunX = 588f
        val sunY = MARGIN
        val sunW = PAGE_WIDTH - MARGIN - sunX
        val sunCols = floatArrayOf(60f, 61f, 61f, 60f)
        painter.cell(sunX, sunY, sunCols[0], 14f, ctx.getString(R.string.field_sun_point), 5.0f, true, Paint.Align.CENTER, true)
        var sx = sunX + sunCols[0]
        painter.cell(sx, sunY, sunCols[1], 14f, ctx.getString(R.string.field_sunrise), 5.0f, true, Paint.Align.CENTER, true)
        sx += sunCols[1]
        painter.cell(sx, sunY, sunCols[2], 14f, ctx.getString(R.string.field_sunset), 5.0f, true, Paint.Align.CENTER, true)
        sx += sunCols[2]
        painter.cell(sx, sunY, sunCols[3], 14f, ctx.getString(R.string.field_sun_sum), 5.0f, true, Paint.Align.CENTER, true)

        journal.sunEntries.take(5).forEachIndexed { index, entry ->
            val y = sunY + 14f + index * 10f
            painter.cell(sunX, y, sunCols[0], 10f, entry.point, 5.2f, false, Paint.Align.LEFT)
            painter.cell(sunX + sunCols[0], y, sunCols[1], 10f, entry.sunrise, 5.2f, false, Paint.Align.CENTER)
            painter.cell(sunX + sunCols[0] + sunCols[1], y, sunCols[2], 10f, entry.sunset, 5.2f, false, Paint.Align.CENTER)
            painter.cell(sunX + sunCols[0] + sunCols[1] + sunCols[2], y, sunCols[3], 10f, entry.sum, 5.2f, false, Paint.Align.CENTER)
        }

        // Aerodrome blocks
        drawAerodromeBlock(
            painter, ctx.getString(R.string.section_departure), journal.departure.name,
            journal.departure.declination, journal.departure.windSpeedKmh,
            journal.departure.pressureMmHg, journal.departure.temperatureC,
            journal.departure.sahPercent, journal.departure.massKg,
            journal.departure.speedKmh, journal.departure.landingSpeedKmh,
            journal.departure.runM, journal.departure.landingRunM,
            journal.departure.safeCircleHeightM, true,
            MARGIN, 80f, 260f, 88f, units, locale
        )
        drawAerodromeBlock(
            painter, ctx.getString(R.string.section_arrival), journal.arrival.name,
            journal.arrival.declination, journal.arrival.windSpeedKmh,
            journal.arrival.pressureMmHg, journal.arrival.temperatureC,
            journal.arrival.sahPercent, journal.arrival.massKg,
            journal.arrival.approachSpeedKmh, journal.arrival.landingSpeedKmh,
            journal.arrival.runM, journal.arrival.landingRunM,
            journal.arrival.safeCircleHeightM, false,
            274f, 80f, 260f, 88f, units, locale
        )

        drawWeatherBlock(
            painter, journal,
            538f, 80f, PAGE_WIDTH - MARGIN - 538f, 88f,
            units, locale
        )

        return 168f
    }

    private fun drawContinuationHeader(
        painter: Painter,
        journal: Journal,
        pageNumber: Int,
        locale: Locale,
        languageTag: String
    ): Float {
        val ctx = painter.context
        painter.text(
            text = ctx.getString(R.string.pdf_title) + " В· " + ctx.getString(R.string.pdf_continuation),
            x = MARGIN,
            y = MARGIN + 10f,
            size = 9f,
            bold = true
        )
        painter.text(
            text = journal.number,
            x = PAGE_WIDTH - MARGIN,
            y = MARGIN + 10f,
            size = 9f,
            bold = true,
            align = Paint.Align.RIGHT
        )
        painter.text(
            text = formatDate(journal.dateIso, languageTag),
            x = PAGE_WIDTH - MARGIN,
            y = MARGIN + 21f,
            size = 6.5f,
            align = Paint.Align.RIGHT
        )
        return 34f
    }

    private fun drawWeatherBlock(
        painter: Painter,
        journal: Journal,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        units: UnitSystem,
        locale: Locale
    ) {
        val ctx = painter.context
        painter.rect(x, y, width, height, fill = Color.WHITE, stroke = 0.5f)
        painter.rect(x, y, width, 14f, fill = Color.rgb(0xEF, 0xEF, 0xEF), stroke = 0.5f)
        painter.text(ctx.getString(R.string.section_weather), x + 4f, y + 10f, 5.8f, true)
        val weather = journal.windWeather
        val cols = 4
        val colW = width / cols
        val rowH = (height - 14f) / 3f
        val pressureDecimals = if (units == UnitSystem.RU) 0 else 2

        val items = listOf(
            ctx.getString(R.string.field_landing_course) to
                degrees(weather.landingCourseMagnetic, locale),
            ctx.getString(R.string.field_friction) to
                num(weather.frictionCoefficient, Quantity.NONE, units, locale, 2),
            ctx.getString(R.string.field_declination) to
                degrees(weather.surfaceWindDirection, locale),
            ctx.getString(R.string.field_decision_height) to
                num(weather.decisionHeightM, Quantity.ALTITUDE, units, locale, 0),
            "U" to num(weather.surfaceWindSpeedKmh, Quantity.SPEED, units, locale, 0),
            ctx.getString(R.string.field_visibility) to
                num(weather.visibilityKm, Quantity.DISTANCE, units, locale, 0),
            ctx.getString(R.string.field_pressure) to
                num(weather.pressureMmHg, Quantity.PRESSURE, units, locale, pressureDecimals),
            ctx.getString(R.string.field_obstacle_height) to
                num(weather.obstacleHeightM, Quantity.ALTITUDE, units, locale, 0),
            ctx.getString(R.string.field_temperature) to
                num(weather.temperatureC, Quantity.TEMPERATURE, units, locale, 0),
            ctx.getString(R.string.field_weather_valid) to
                weatherValid(weather.weatherValidHours, weather.weatherValidMinutes, ctx),
            ctx.getString(R.string.field_min_pressure) to
                num(weather.minPressureMmHg, Quantity.PRESSURE, units, locale, pressureDecimals),
            ctx.getString(R.string.field_assigned_level) to
                num(weather.assignedLevelM, Quantity.ALTITUDE, units, locale, 0)
        )

        items.forEachIndexed { index, (label, value) ->
            val col = index % cols
            val row = index / cols
            val cx = x + col * colW
            val cy = y + 14f + row * rowH
            painter.fieldCell(cx, cy, colW, rowH, label, value)
        }
    }

    private fun drawAerodromeBlock(
        painter: Painter,
        title: String,
        name: String,
        declination: Double?,
        windSpeed: Double?,
        pressure: Double?,
        temperature: Double?,
        sah: Double?,
        mass: Double?,
        speed: Double?,
        landingSpeed: Double?,
        run: Double?,
        landingRun: Double?,
        safeCircle: Double?,
        departure: Boolean,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        units: UnitSystem,
        locale: Locale
    ) {
        val ctx = painter.context
        painter.rect(x, y, width, height, fill = Color.WHITE, stroke = 0.5f)
        painter.rect(x, y, width, 14f, fill = Color.rgb(0xEF, 0xEF, 0xEF), stroke = 0.5f)
        painter.text(title, x + 4f, y + 10f, 5.8f, true)
        painter.text(
            text = name,
            x = x + width - 4f,
            y = y + 10f,
            size = 6.5f,
            align = Paint.Align.RIGHT,
            maxWidth = width - 90f
        )
        val pressureDecimals = if (units == UnitSystem.RU) 0 else 2
        val colW = width / 4f
        val rowH = (height - 14f) / 3f
        val fields = mutableListOf<Pair<String, String>>()
        fields += ctx.getString(R.string.field_declination) to degrees(declination, locale)
        fields += "U" to num(windSpeed, Quantity.SPEED, units, locale, 0)
        fields += ctx.getString(R.string.field_p0) to num(pressure, Quantity.PRESSURE, units, locale, pressureDecimals)
        fields += ctx.getString(R.string.field_t0) to num(temperature, Quantity.TEMPERATURE, units, locale, 0)
        fields += ctx.getString(R.string.field_sah) to num(sah, Quantity.NONE, units, locale, 1)
        fields += if (departure) {
            ctx.getString(R.string.field_takeoff_mass) to num(mass, Quantity.MASS, units, locale, 0)
        } else {
            ctx.getString(R.string.field_landing_mass) to num(mass, Quantity.MASS, units, locale, 0)
        }
        if (departure) {
            fields += ctx.getString(R.string.field_takeoff_speed) to num(speed, Quantity.SPEED, units, locale, 0)
            fields += ctx.getString(R.string.field_takeoff_run) to num(run, Quantity.ALTITUDE, units, locale, 0)
            fields += ctx.getString(R.string.field_safe_circle) to num(safeCircle, Quantity.ALTITUDE, units, locale, 0)
        } else {
            fields += ctx.getString(R.string.field_approach_speed) to num(speed, Quantity.SPEED, units, locale, 0)
            fields += ctx.getString(R.string.field_landing_speed) to num(landingSpeed, Quantity.SPEED, units, locale, 0)
            fields += ctx.getString(R.string.field_landing_run) to num(landingRun, Quantity.ALTITUDE, units, locale, 0)
            fields += ctx.getString(R.string.field_safe_circle) to num(safeCircle, Quantity.ALTITUDE, units, locale, 0)
        }
        fields.take(12).forEachIndexed { index, (label, value) ->
            val col = index % 4
            val row = index / 4
            painter.fieldCell(
                x + col * colW,
                y + 14f + row * rowH,
                colW,
                rowH,
                label,
                value
            )
        }
    }

    private fun drawRouteTable(
        painter: Painter,
        rows: List<PdfRow>,
        calc: JournalCalculation,
        journal: Journal,
        top: Float,
        units: UnitSystem,
        locale: Locale,
        languageTag: String
    ) {
        val ctx = painter.context
        val columns = tableColumns(units, ctx)
        val widths = columns.map { it.width }.toFloatArray()
        val totalWidth = widths.sum()
        val scale = CONTENT_WIDTH / totalWidth
        val scaledWidths = widths.map { it * scale }.toFloatArray()

        // header
        painter.rect(MARGIN, top, CONTENT_WIDTH, TABLE_HEADER_HEIGHT, fill = Color.rgb(0xEF, 0xEF, 0xEF), stroke = 0.5f)
        var x = MARGIN
        columns.forEachIndexed { index, column ->
            val w = scaledWidths[index]
            if (index > 0) painter.line(x, top, x, top + TABLE_HEADER_HEIGHT, 0.4f)
            painter.cellLines(
                x = x,
                y = top,
                w = w,
                h = TABLE_HEADER_HEIGHT,
                text = column.header,
                size = 5.0f,
                bold = true,
                align = Paint.Align.CENTER
            )
            x += w
        }

        val altUnit = altitudeUnit(units, ctx)
        val speedUnit = speedUnit(units, ctx)
        val distanceUnit = distanceUnit(units, ctx)
        val massUnit = massUnit(units, ctx)

        rows.forEachIndexed { rowIndex, row ->
            val y = top + TABLE_HEADER_HEIGHT + rowIndex * ROW_HEIGHT
            painter.rect(MARGIN, y, CONTENT_WIDTH, ROW_HEIGHT, fill = Color.WHITE, stroke = 0.4f)
            var cx = MARGIN
            val routeText = when {
                row.isTakeoff -> ctx.getString(R.string.field_takeoff)
                row.isLanding && row.route.isBlank() -> ctx.getString(R.string.field_landing)
                else -> row.route
            }
            val values = listOf(
                num(row.reliefM, Quantity.ALTITUDE, units, locale, 0),
                num(row.obstacleM, Quantity.ALTITUDE, units, locale, 0),
                num(row.safeAltM, Quantity.ALTITUDE, units, locale, 0),
                routeText,
                degrees(row.mpu, locale),
                degrees(row.mk, locale),
                num(row.tas, Quantity.SPEED, units, locale, 0),
                num(row.gs, Quantity.SPEED, units, locale, 0),
                num(row.distance, Quantity.DISTANCE, units, locale, 0),
                time(row.timeMinutes),
                num(row.fuelCalc, Quantity.MASS, units, locale, 1),
                num(row.fuelActual, Quantity.MASS, units, locale, 1),
                time(row.calcTimeMinutes),
                time(row.actualTimeMinutes),
                num(row.flightLevelM, Quantity.ALTITUDE, units, locale, 0),
                row.notes
            )
            columns.forEachIndexed { index, column ->
                val w = scaledWidths[index]
                if (index > 0) painter.line(cx, y, cx, y + ROW_HEIGHT, 0.4f)
                val align = if (index == 3 || index == 15) Paint.Align.LEFT else Paint.Align.CENTER
                painter.cell(
                    x = cx,
                    y = y,
                    w = w,
                    h = ROW_HEIGHT,
                    text = values[index],
                    size = if (index == 15) 5.0f else 6.4f,
                    bold = row.isTakeoff || row.isLanding,
                    align = align
                )
                cx += w
            }
        }

    }

    private fun drawTotals(
        painter: Painter,
        calc: JournalCalculation,
        top: Float,
        units: UnitSystem,
        locale: Locale,
        languageTag: String
    ) {
        val ctx = painter.context
        val columns = tableColumns(units, ctx)
        val widths = columns.map { it.width }.toFloatArray()
        val scale = CONTENT_WIDTH / widths.sum()
        val scaled = widths.map { it * scale }.toFloatArray()

        painter.rect(MARGIN, top, CONTENT_WIDTH, ROW_HEIGHT, fill = Color.rgb(0xF6, 0xF6, 0xF6), stroke = 0.5f)

        val sX = MARGIN + scaled.take(8).sum()
        val tX = sX + scaled[8]
        val qX = tX + scaled[9]
        val qEnd = qX + scaled[10]
        painter.line(sX, top, sX, top + ROW_HEIGHT, 0.4f)
        painter.line(tX, top, tX, top + ROW_HEIGHT, 0.4f)
        painter.line(qX, top, qX, top + ROW_HEIGHT, 0.4f)
        painter.line(qEnd, top, qEnd, top + ROW_HEIGHT, 0.4f)

        painter.text(
            text = ctx.getString(R.string.field_total_distance),
            x = MARGIN + 4f,
            y = top + ROW_HEIGHT / 2f + 2f,
            size = 6.5f,
            bold = true,
            maxWidth = sX - MARGIN - 8f
        )

        painter.text(
            text = num(calc.totalDistanceKm, Quantity.DISTANCE, units, locale, 0),
            x = sX + scaled[8] / 2f,
            y = top + ROW_HEIGHT / 2f + 2f,
            size = 6.3f,
            bold = true,
            align = Paint.Align.CENTER
        )
        painter.text(
            text = time(calc.totalTimeMinutes),
            x = tX + scaled[9] / 2f,
            y = top + ROW_HEIGHT / 2f + 2f,
            size = 6.3f,
            bold = true,
            align = Paint.Align.CENTER
        )
        painter.text(
            text = num(calc.totalFuelKg, Quantity.MASS, units, locale, 1),
            x = qX + scaled[10] / 2f,
            y = top + ROW_HEIGHT / 2f + 2f,
            size = 6.3f,
            bold = true,
            align = Paint.Align.CENTER
        )
    }

    private fun drawBottomBand(
    painter: Painter,
    journal: Journal,
    pilots: List<Pilot>,
    units: UnitSystem,
    locale: Locale,
    languageTag: String
) {
    val ctx = painter.context
    val top = BOTTOM_BAND_TOP
    val height = PAGE_HEIGHT - top - 12f
    painter.rect(MARGIN, top, CONTENT_WIDTH, height, fill = Color.WHITE, stroke = 0.5f)
    painter.line(MARGIN + 480f, top, MARGIN + 480f, top + height, 0.4f)
    painter.line(MARGIN, top + 20f, MARGIN + CONTENT_WIDTH, top + 20f, 0.3f)
    painter.line(MARGIN, top + 42f, MARGIN + CONTENT_WIDTH, top + 42f, 0.3f)

    val leftX = MARGIN + 4f
    val actual = journal.actual

    drawSQRow(
        painter = painter,
        label = ctx.getString(R.string.field_go_around),
        sLabel = ctx.getString(R.string.field_distance_s),
        qLabel = ctx.getString(R.string.field_fuel_q),
        sValue = num(actual.goAroundDistanceKm, Quantity.DISTANCE, units, locale, 0),
        qValue = num(actual.goAroundFuelKg, Quantity.MASS, units, locale, 1),
        x = leftX,
        y = top + 13f
    )
    drawSQRow(
        painter = painter,
        label = ctx.getString(R.string.field_diversion),
        sLabel = ctx.getString(R.string.field_diversion_s),
        qLabel = ctx.getString(R.string.field_diversion_q),
        sValue = num(actual.diversionDistanceKm, Quantity.DISTANCE, units, locale, 0),
        qValue = num(actual.diversionFuelKg, Quantity.MASS, units, locale, 1),
        x = leftX,
        y = top + 30f
    )

    painter.text(
        text = ctx.getString(R.string.field_preflight_check),
        x = leftX,
        y = top + 54f,
        size = 5.4f,
        maxWidth = 470f
    )

    val duty = pilotName(journal.pilots.dutyNavigatorId, pilots)
    painter.text(
        text = ctx.getString(R.string.field_duty_navigator),
        x = leftX,
        y = top + 70f,
        size = 5.4f,
        maxWidth = 100f
    )
    painter.line(leftX + 92f, top + 73f, leftX + 300f, top + 73f, 0.4f)
    if (duty.isNotBlank()) {
        painter.text(
            text = duty,
            x = leftX + 94f,
            y = top + 70f,
            size = 6.5f,
            maxWidth = 200f
        )
    }

    val checkDate = formatDate(
        journal.preflightDateIso.ifBlank { journal.dateIso },
        languageTag
    )
    painter.text(
        text = checkDate + if (languageTag == "en") " y." else " г.",
        x = leftX + 315f,
        y = top + 70f,
        size = 5.4f,
        maxWidth = 150f
    )

    val rightX = MARGIN + 488f
    val rightW = CONTENT_WIDTH - 488f - 4f
    drawTimeRow(
        painter = painter,
        label = ctx.getString(R.string.field_actual_flight_time),
        minutes = actual.actualTimeMinutes,
        x = rightX,
        y = top + 13f,
        width = rightW,
        ctx = ctx
    )
    drawTimeRow(
        painter = painter,
        label = ctx.getString(R.string.field_night_time),
        minutes = actual.nightTimeMinutes,
        x = rightX,
        y = top + 30f,
        width = rightW,
        ctx = ctx
    )

    val signatures = listOf(
        ctx.getString(R.string.field_commander) to pilotName(journal.pilots.commanderId, pilots),
        ctx.getString(R.string.field_second_pilot) to pilotName(journal.pilots.secondPilotId, pilots),
        ctx.getString(R.string.field_squadron_navigator) to pilotName(journal.pilots.squadronNavigatorId, pilots)
    )
    signatures.forEachIndexed { index, (label, name) ->
        val y = top + 54f + index * 15f
        painter.text(label, rightX, y, 5.2f, false, Color.DKGRAY, Paint.Align.LEFT, 110f)
        painter.line(rightX + 112f, y + 3f, rightX + rightW, y + 3f, 0.4f)
        if (name.isNotBlank()) {
            painter.text(
                text = name,
                x = rightX + 114f,
                y = y,
                size = 6.5f,
                false,
                Color.BLACK,
                Paint.Align.LEFT,
                rightW - 118f
            )
        }
    }
}

private fun drawSQRow(
    painter: Painter,
    label: String,
    sLabel: String,
    qLabel: String,
    sValue: String,
    qValue: String,
    x: Float,
    y: Float
) {
    painter.text(label, x, y, 4.9f, false, Color.DKGRAY, Paint.Align.LEFT, 235f)
    drawSQField(painter, sLabel, sValue, x + 245f, y)
    drawSQField(painter, qLabel, qValue, x + 360f, y)
}

private fun drawSQField(
    painter: Painter,
    label: String,
    value: String,
    x: Float,
    y: Float
) {
    val labelWidth = if (label.length > 2) 30f else 12f
    painter.text(label, x, y, 5.2f, true, Color.BLACK, Paint.Align.LEFT, 40f)
    val lineStart = x + labelWidth
    painter.line(lineStart, y + 3f, lineStart + 70f, y + 3f, 0.4f)
    if (value.isNotBlank()) {
        painter.text(
            text = value,
            x = lineStart + 2f,
            y = y,
            size = 6.5f,
            align = Paint.Align.LEFT,
            maxWidth = 66f
        )
    }
}

private fun drawTimeRow(
    painter: Painter,
    label: String,
    minutes: Int?,
    x: Float,
    y: Float,
    width: Float,
    ctx: Context
) {
    painter.text(
        text = label,
        x = x,
        y = y,
        size = 5.4f,
        color = Color.DKGRAY,
        align = Paint.Align.LEFT,
        maxWidth = width - 145f
    )

    val hours = minutes?.div(60)
    val mins = minutes?.rem(60)
    val hx = x + width - 135f
    val mx = x + width - 60f

    painter.text(
        text = hours?.toString() ?: "",
        x = hx,
        y = y,
        size = 6.4f,
        align = Paint.Align.RIGHT,
        maxWidth = 40f
    )
    painter.line(hx - 22f, y + 3f, hx + 2f, y + 3f, 0.4f)
    painter.text(
        text = ctx.getString(R.string.unit_hour),
        x = hx + 8f,
        y = y,
        size = 5.4f,
        align = Paint.Align.LEFT,
        maxWidth = 30f
    )

    painter.text(
        text = mins?.toString() ?: "",
        x = mx,
        y = y,
        size = 6.4f,
        align = Paint.Align.RIGHT,
        maxWidth = 40f
    )
    painter.line(mx - 22f, y + 3f, mx + 2f, y + 3f, 0.4f)
    painter.text(
        text = ctx.getString(R.string.unit_min),
        x = mx + 8f,
        y = y,
        size = 5.4f,
        align = Paint.Align.LEFT,
        maxWidth = 35f
    )
}

private fun tableColumns(units: UnitSystem, ctx: Context): List<TableColumn> {
        val altUnit = altitudeUnit(units, ctx)
        val speedUnit = speedUnit(units, ctx)
        val distanceUnit = distanceUnit(units, ctx)
        val massUnit = massUnit(units, ctx)
        return listOf(
            TableColumn(ctx.getString(R.string.field_relief_height) + "\n" + altUnit, 28f),
            TableColumn(ctx.getString(R.string.field_obstacle_height) + "\n" + altUnit, 32f),
            TableColumn(ctx.getString(R.string.field_safe_alt) + "\n" + altUnit, 34f),
            TableColumn(ctx.getString(R.string.field_route), 140f),
            TableColumn(ctx.getString(R.string.field_magnetic_track) + "\n°", 24f),
            TableColumn(ctx.getString(R.string.field_magnetic_heading) + "\n°", 24f),
            TableColumn(ctx.getString(R.string.field_true_airspeed) + "\n" + speedUnit, 26f),
            TableColumn(ctx.getString(R.string.field_ground_speed) + "\n" + speedUnit, 24f),
            TableColumn(ctx.getString(R.string.field_distance) + "\n" + distanceUnit, 24f),
            TableColumn(ctx.getString(R.string.field_time) + "\nh:mm", 26f),
            TableColumn(ctx.getString(R.string.field_fuel_calc) + "\n" + massUnit, 32f),
            TableColumn(ctx.getString(R.string.field_fuel_actual) + "\n" + massUnit, 32f),
            TableColumn(ctx.getString(R.string.field_calc_time) + "\nh:mm", 28f),
            TableColumn(ctx.getString(R.string.field_actual_time) + "\nh:mm", 28f),
            TableColumn(ctx.getString(R.string.field_flight_level) + "\n" + altUnit, 28f),
            TableColumn(ctx.getString(R.string.field_notes), 284f)
        )
    }

    private fun num(
        value: Double?,
        quantity: Quantity,
        units: UnitSystem,
        locale: Locale,
        maxDecimals: Int
    ): String = UnitConverter.format(value, quantity, units, locale, maxDecimals)

    private fun dateOrBlank(value: String?, languageTag: String): String =
        if (value.isNullOrBlank()) "" else formatDate(value, languageTag)

    private fun time(minutes: Int?): String = if (minutes == null) "" else formatMinutes(minutes)

    private fun degrees(value: Double?, locale: Locale): String =
        if (value == null) "" else UnitConverter.formatPlain(value, locale, 0) + "°"

    private fun weatherValid(hours: Int?, minutes: Int?, ctx: Context): String {
        val h = hours?.toString() ?: "—"
        val m = minutes?.toString() ?: "—"
        return "$h ${ctx.getString(R.string.unit_hour)} $m ${ctx.getString(R.string.unit_min)}"
    }

    private fun pilotName(id: Long?, pilots: List<Pilot>): String =
        pilots.firstOrNull { it.id == id }?.name ?: ""

    private fun altitudeUnit(units: UnitSystem, ctx: Context): String =
        ctx.getString(if (units == UnitSystem.RU) R.string.unit_m else R.string.unit_ft)

    private fun speedUnit(units: UnitSystem, ctx: Context): String =
        ctx.getString(if (units == UnitSystem.RU) R.string.unit_kmh else R.string.unit_kt)

    private fun distanceUnit(units: UnitSystem, ctx: Context): String =
        ctx.getString(if (units == UnitSystem.RU) R.string.unit_km else R.string.unit_nm)

    private fun massUnit(units: UnitSystem, ctx: Context): String =
        ctx.getString(if (units == UnitSystem.RU) R.string.unit_kg else R.string.unit_lb)

    private fun pressureUnit(units: UnitSystem, ctx: Context): String =
        ctx.getString(if (units == UnitSystem.RU) R.string.unit_mmhg else R.string.unit_inhg)

    private fun temperatureUnit(units: UnitSystem, ctx: Context): String =
        ctx.getString(if (units == UnitSystem.RU) R.string.unit_c else R.string.unit_f)

    private data class TableColumn(val header: String, val width: Float)

    private data class PdfRow(
        val route: String,
        val reliefM: Double?,
        val obstacleM: Double?,
        val safeAltM: Double?,
        val mpu: Double?,
        val mk: Double?,
        val tas: Double?,
        val gs: Double?,
        val distance: Double?,
        val timeMinutes: Int?,
        val fuelCalc: Double?,
        val fuelActual: Double?,
        val calcTimeMinutes: Int?,
        val actualTimeMinutes: Int?,
        val flightLevelM: Double?,
        val notes: String,
        val isTakeoff: Boolean = false,
        val isLanding: Boolean = false
    ) {
        companion object {
            fun takeoff(journal: Journal, calc: JournalCalculation) = PdfRow(
                route = "",
                reliefM = null,
                obstacleM = null,
                safeAltM = null,
                mpu = null,
                mk = null,
                tas = null,
                gs = null,
                distance = null,
                timeMinutes = journal.takeoffTimeMinutes,
                fuelCalc = calc.takeoffFuelKg,
                fuelActual = null,
                calcTimeMinutes = null,
                actualTimeMinutes = null,
                flightLevelM = null,
                notes = "",
                isTakeoff = true
            )

            fun landing(journal: Journal, calc: JournalCalculation) = PdfRow(
                route = journal.landingPoint.ifBlank { journal.arrival.name },
                reliefM = null,
                obstacleM = null,
                safeAltM = null,
                mpu = null,
                mk = null,
                tas = null,
                gs = null,
                distance = null,
                timeMinutes = journal.landingTimeMinutes,
                fuelCalc = calc.landingFuelKg,
                fuelActual = null,
                calcTimeMinutes = null,
                actualTimeMinutes = null,
                flightLevelM = null,
                notes = "",
                isLanding = true
            )

            fun leg(leg: Leg, calc: com.yc.navigator.domain.LegCalculation?, index: Int) = PdfRow(
                route = leg.route,
                reliefM = leg.reliefHeightM,
                obstacleM = leg.obstacleHeightM,
                safeAltM = calc?.safeAltitudeM,
                mpu = leg.magneticTrack,
                mk = calc?.magneticCourseDeg,
                tas = leg.trueAirspeedKmh,
                gs = calc?.groundSpeedKmh,
                distance = leg.distanceKm,
                timeMinutes = calc?.timeMinutes,
                fuelCalc = calc?.fuelRemainingKg,
                fuelActual = leg.fuelActualKg,
                calcTimeMinutes = leg.calcTimeMinutes,
                actualTimeMinutes = leg.actualTimeMinutes,
                flightLevelM = leg.flightLevelM,
                notes = leg.notes
            )
        }
    }

    private class Painter(
        val canvas: Canvas,
        val context: Context,
        val locale: Locale,
        val units: UnitSystem
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create("serif", Typeface.NORMAL)
        }
        private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = Typeface.create("serif", Typeface.BOLD)
        }

        fun text(
            text: String,
            x: Float,
            y: Float,
            size: Float,
            bold: Boolean = false,
            color: Int = Color.BLACK,
            align: Paint.Align = Paint.Align.LEFT,
            maxWidth: Float = Float.MAX_VALUE
        ) {
            val p = if (bold) boldPaint else paint
            p.color = color
            p.textSize = size
            p.textAlign = align
            if (!drawWords(text, x, y, p, maxWidth, align)) {
                val value = if (maxWidth < Float.MAX_VALUE) ellipsize(text, p, maxWidth) else text
                canvas.drawText(value, x, y, p)
            }
        }

        private fun drawWords(
            text: String,
            x: Float,
            y: Float,
            p: Paint,
            maxWidth: Float,
            align: Paint.Align
        ): Boolean {
            if (!text.contains(' ') || p.textSize > 7.0f) return false
            val words = text.split(' ').filter { it.isNotEmpty() }
            if (words.size < 2) return false
            val gap = p.textSize * 0.45f
            val widths = words.map { p.measureText(it) }
            val total = widths.sum() + gap * (words.size - 1)
            if (maxWidth < Float.MAX_VALUE && total > maxWidth) return false
            var cursor = when (align) {
                Paint.Align.CENTER -> x - total / 2f
                Paint.Align.RIGHT -> x - total
                else -> x
            }
            words.forEachIndexed { index, word ->
                canvas.drawText(word, cursor, y, p)
                cursor += widths[index] + gap
            }
            return true
        }

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, width: Float = 0.4f) {
            paint.color = Color.DKGRAY
            paint.strokeWidth = width
            paint.style = Paint.Style.STROKE
            paint.textAlign = Paint.Align.LEFT
            canvas.drawLine(x1, y1, x2, y2, paint)
            paint.style = Paint.Style.FILL
        }

        fun rect(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            fill: Int? = null,
            stroke: Float = 0.4f
        ) {
            if (fill != null) {
                paint.color = fill
                paint.style = Paint.Style.FILL
                canvas.drawRect(x, y, x + w, y + h, paint)
            }
            if (stroke > 0f) {
                paint.color = Color.DKGRAY
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = stroke
                canvas.drawRect(x, y, x + w, y + h, paint)
                paint.style = Paint.Style.FILL
            }
        }

        fun cell(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            text: String,
            size: Float,
            bold: Boolean = false,
            align: Paint.Align = Paint.Align.LEFT,
            fill: Boolean = false
        ) {
            if (fill) rect(x, y, w, h, Color.rgb(0xEF, 0xEF, 0xEF), 0.4f)
            else rect(x, y, w, h, null, 0.4f)
            val p = if (bold) boldPaint else paint
            p.color = Color.BLACK
            p.textSize = size
            p.textAlign = align
            val padding = 3f
            val available = max(2f, w - padding * 2)
            val cx = when (align) {
                Paint.Align.CENTER -> x + w / 2f
                Paint.Align.RIGHT -> x + w - padding
                else -> x + padding
            }
            val baseline = y + h / 2f - (p.ascent() + p.descent()) / 2f
            if (!drawWords(text, cx, baseline, p, available, align)) {
                val value = ellipsize(text, p, available)
                canvas.drawText(value, cx, baseline, p)
            }
        }

        fun cellLines(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            text: String,
            size: Float,
            bold: Boolean = false,
            align: Paint.Align = Paint.Align.CENTER
        ) {
            rect(x, y, w, h, null, 0.4f)
            val p = if (bold) boldPaint else paint
            p.color = Color.BLACK
            p.textSize = size
            p.textAlign = align
            val lines = text.split('\n')
            val lineHeight = size + 1.8f
            val total = lines.size * lineHeight
            var baseline = y + (h - total) / 2f + size
            val padding = 2f
            lines.forEach { line ->
                val available = max(2f, w - padding * 2)
                val cx = when (align) {
                    Paint.Align.CENTER -> x + w / 2f
                    Paint.Align.RIGHT -> x + w - padding
                    else -> x + padding
                }
                if (!drawWords(line, cx, baseline, p, available, align)) {
                    val value = ellipsize(line, p, available)
                    canvas.drawText(value, cx, baseline, p)
                }
                baseline += lineHeight
            }
        }

        fun cellSubscript(
            x: Float,
            y: Float,
            w: Float,
            h: Float,
            main: String,
            sub: String,
            size: Float = 5.4f,
            subSize: Float = 3.8f,
            bold: Boolean = false
        ) {
            rect(x, y, w, h, null, 0.4f)
            val p = if (bold) boldPaint else paint
            p.color = Color.BLACK
            p.textAlign = Paint.Align.LEFT

            p.textSize = size
            val mainWidth = p.measureText(main)
            p.textSize = subSize
            val subWidth = p.measureText(sub)
            val startX = x + (w - mainWidth - subWidth) / 2f
            val baseline = y + h / 2f - (p.ascent() + p.descent()) / 2f

            p.textSize = size
            canvas.drawText(main, startX, baseline, p)
            p.textSize = subSize
            canvas.drawText(sub, startX + mainWidth, baseline + 2f, p)
        }

        fun fieldCell(x: Float, y: Float, w: Float, h: Float, label: String, value: String) {
            line(x, y, x + w, y, 0.4f)
            text(label, x + 3f, y + 6.5f, 4.5f, false, Color.DKGRAY, Paint.Align.LEFT, w - 6f)
            text(value, x + 3f, y + h - 4f, 6.8f, false, Color.BLACK, Paint.Align.LEFT, w - 6f)
        }

        fun inlineField(
            label: String,
            value: String,
            x: Float,
            y: Float,
            labelWidth: Float,
            width: Float
        ) {
            text(label, x, y, 4.8f, false, Color.DKGRAY, Paint.Align.LEFT, labelWidth)
            val valueX = x + labelWidth
            line(valueX, y + 3.5f, x + width, y + 3.5f, 0.4f)
            if (value.isNotBlank()) {
                text(
                    value,
                    valueX + 2f,
                    y,
                    6.4f,
                    false,
                    Color.BLACK,
                    Paint.Align.LEFT,
                    x + width - valueX - 4f
                )
            }
        }

        private fun ellipsize(text: String, p: Paint, maxWidth: Float): String {
            if (text.isEmpty() || maxWidth <= 0f) return ""
            if (p.measureText(text) <= maxWidth) return text
            var low = 0
            var high = text.length
            while (low < high) {
                val mid = (low + high + 1) / 2
                if (p.measureText(text.substring(0, mid) + "…") <= maxWidth) {
                    low = mid
                } else {
                    high = mid - 1
                }
            }
            return if (low <= 0) "" else text.substring(0, low) + "…"
        }
    }
}







