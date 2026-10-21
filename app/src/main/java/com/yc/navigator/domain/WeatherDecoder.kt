package com.yc.navigator.domain

import com.yc.navigator.data.WeatherReportType
import java.util.Locale
import kotlin.math.roundToInt

data class WeatherLine(
    val label: String,
    val value: String
)

data class WeatherDecodedSection(
    val title: String,
    val lines: List<WeatherLine>
)

fun decodeWeatherSections(
    type: String,
    raw: String,
    languageTag: String
): List<WeatherDecodedSection> {
    if (raw.isBlank()) return emptyList()
    if (type != WeatherReportType.METAR_TAF.key) {
        return listOf(WeatherDecodedSection(type, decodeWeather(type, raw, languageTag)))
    }

    val chunks = mutableListOf<Pair<String, StringBuilder>>()
    raw.lines().forEach { line ->
        val trimmed = line.trim()
        val chunkType = when {
            trimmed.startsWith("TAF", ignoreCase = true) -> "TAF"
            trimmed.startsWith("METAR", ignoreCase = true) ||
                trimmed.startsWith("SPECI", ignoreCase = true) -> "METAR"
            else -> null
        }
        if (chunkType != null) {
            chunks += chunkType to StringBuilder(line)
        } else if (chunks.isNotEmpty()) {
            chunks.last().second.append('\n').append(line)
        }
    }

    if (chunks.isEmpty()) {
        return listOf(
            WeatherDecodedSection("METAR", decodeWeather("METAR", raw, languageTag))
        )
    }

    return chunks.map { (chunkType, text) ->
        WeatherDecodedSection(
            title = chunkType,
            lines = decodeWeather(chunkType, text.toString(), languageTag)
        )
    }.filter { it.lines.isNotEmpty() }
}

fun decodeWeather(
    type: String,
    raw: String,
    languageTag: String
): List<WeatherLine> {
    if (raw.isBlank()) return emptyList()

    val english = languageTag == "en"
    fun tr(ru: String, en: String): String = if (english) en else ru

    val lines = mutableListOf<WeatherLine>()
    fun add(labelRu: String, labelEn: String, value: String) {
        if (value.isNotBlank()) {
            lines += WeatherLine(tr(labelRu, labelEn), value)
        }
    }

    val normalized = raw.uppercase(Locale.US).replace('\n', ' ')
    val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }

    val station = tokens.firstOrNull {
        Regex("^[A-Z]{4}$").matches(it) && it !in EXCLUDED_STATION_WORDS
    }
    if (station != null) {
        add("Станция", "Station", station)
    }

    Regex("\\b(\\d{2})(\\d{2})(\\d{2})Z\\b").find(normalized)?.let { match ->
        add(
            "Время",
            "Time",
            "${match.groupValues[1]} ${match.groupValues[2]}:${match.groupValues[3]} UTC"
        )
    }

    if (type == "TAF") {
        Regex("\\b(\\d{4})/(\\d{4})\\b").find(normalized)?.let { match ->
            add(
                "Период действия",
                "Validity",
                "${tr("с", "from")} ${formatDayHour(match.groupValues[1])} " +
                    "${tr("по", "to")} ${formatDayHour(match.groupValues[2])} UTC"
            )
        }
    } else {
        Regex("VALID\\s+(\\d{4,6})/(\\d{4,6})").find(normalized)?.let { match ->
            add(
                "Период действия",
                "Validity",
                "${tr("с", "from")} ${formatValidity(match.groupValues[1])} " +
                    "${tr("по", "to")} ${formatValidity(match.groupValues[2])} UTC"
            )
        }
    }

    WIND_REGEX.findAll(normalized).forEach { match ->
        val direction = match.groupValues[1]
        val speed = match.groupValues[2]
        val gust = match.groupValues[3].removePrefix("G")
        val unit = windUnit(match.groupValues[4], english)
        val directionText = if (direction == "VRB") {
            tr("переменный", "variable")
        } else {
            "$direction°"
        }
        val gustText = if (gust.isNotBlank()) {
            ", ${tr("порывы", "gusts")} $gust $unit"
        } else {
            ""
        }
        add(
            "Ветер",
            "Wind",
            "$directionText, $speed $unit$gustText"
        )
    }

    if (normalized.contains("CAVOK")) {
        add(
            "Видимость",
            "Visibility",
            tr(
                "CAVOK: 10 км и более, без значительной облачности и осадков",
                "CAVOK: 10 km or more, no significant cloud and no precipitation"
            )
        )
    } else {
        val visibilityToken = tokens.firstOrNull { Regex("^\\d{4}$").matches(it) }
        if (visibilityToken != null) {
            add("Видимость", "Visibility", visibilityText(visibilityToken, english))
        }
        Regex("\\bVIS\\s+(\\d{3,4})M?\\b").find(normalized)?.let { match ->
            add("Видимость", "Visibility", visibilityText(match.groupValues[1], english))
        }
    }

    RVR_REGEX.findAll(normalized).forEach { match ->
        val runway = match.groupValues[1]
        val distance = match.groupValues[2].removePrefix("M").removePrefix("P").toIntOrNull()
        val tendency = when (match.groupValues[3]) {
            "U" -> tr(" (улучшение)", " (improving)")
            "D" -> tr(" (ухудшение)", " (deteriorating)")
            "N" -> tr(" (без изменений)", " (no change)")
            else -> ""
        }
        if (distance != null) {
            add("Видимость на ВПП", "RVR", "ВПП $runway: $distance ${tr("м", "m")}$tendency")
        }
    }

    WEATHER_TOKEN_REGEX.findAll(normalized).forEach { match ->
        val original = match.value
        val vicinity = original.startsWith("VC")
        val intensity = when {
            original.contains("+") -> tr("сильный ", "heavy ")
            original.contains("-") -> tr("слабый ", "light ")
            vicinity -> tr("в районе ", "vicinity ")
            else -> ""
        }
        val core = original
            .removePrefix("VC")
            .replace("+", "")
            .replace("-", "")
        val description = core.chunked(2)
            .mapNotNull { WEATHER_CODES[it] }
            .joinToString(", ") { tr(it.first, it.second) }
        if (description.isNotBlank()) {
            add("Погодные явления", "Weather", intensity + description)
        }
    }

    CLOUD_REGEX.findAll(normalized).forEach { match ->
        val cover = CLOUD_COVERS[match.groupValues[1]]
        val height = match.groupValues[2].toIntOrNull()?.let { hundreds ->
            (hundreds * 100 * 0.3048).roundToInt()
        }
        val cloudType = when (match.groupValues[3]) {
            "CB" -> ", ${tr("кучево-дождевые", "cumulonimbus")}"
            "TCU" -> ", ${tr("мощно-кучевые", "towering cumulus")}"
            else -> ""
        }
        if (cover != null && height != null) {
            add(
                "Облачность",
                "Clouds",
                "${tr(cover.first, cover.second)}, $height ${tr("м", "m")}$cloudType"
            )
        }
    }

    if (tokens.any { it in CLEAR_CLOUD_WORDS }) {
        add(
            "Облачность",
            "Clouds",
            tr("нет значительной облачности", "no significant cloud")
        )
    }

    TEMPERATURE_REGEX.find(normalized)?.let { match ->
        val temperature = parseTemperature(match.groupValues[1])
        val dewPoint = parseTemperature(match.groupValues[2])
        if (temperature != null && dewPoint != null) {
            add(
                "Температура / точка росы",
                "Temperature / dew point",
                "$temperature °C / $dewPoint °C"
            )
        }
    }

    Regex("\\bQ(\\d{3,4})\\b").find(normalized)?.let { match ->
        val hpa = match.groupValues[1].toIntOrNull()
        if (hpa != null) {
            add(
                "Давление",
                "Pressure",
                "$hpa ${tr("гПа", "hPa")}"
            )
        }
    }
    Regex("\\bA(\\d{4})\\b").find(normalized)?.let { match ->
        val rawValue = match.groupValues[1].toIntOrNull()
        if (rawValue != null) {
            val inhg = rawValue / 100.0
            val hpa = (inhg * 33.8639).roundToInt()
            add(
                "Давление",
                "Pressure",
                "%.2f inHg (~$hpa ${tr("гПа", "hPa")})".format(Locale.US, inhg)
            )
        }
    }

    val trends = mutableListOf<String>()
    if ("NOSIG" in tokens) trends += tr("без существенных изменений", "no significant change")
    if ("BECMG" in tokens) trends += tr("постепенно", "becoming")
    if ("TEMPO" in tokens) trends += tr("временно", "temporary")
    if ("INTER" in tokens) trends += tr("кратковременно", "intermittent")
    if (tokens.any { it.startsWith("PROB") }) {
        trends += tokens.filter { it.startsWith("PROB") }.joinToString(", ")
    }
    if (trends.isNotEmpty()) {
        add("Изменения", "Trend", trends.joinToString(", "))
    }

    if (type == "SIGMET" || type == "AIRMET" || type == "GAMET") {
        Regex("\\b(?:FIR|UIR|AREA)\\s+([A-Z0-9]{2,8})\\b").find(normalized)?.let { match ->
            add("Район", "Area", match.groupValues[1])
        }

        val levels = Regex("\\bFL(\\d{2,3})\\b").findAll(normalized)
            .mapNotNull { it.groupValues[1].toIntOrNull() }
            .map { level ->
                val meters = (level * 100 * 0.3048).roundToInt()
                "FL$level (~$meters ${tr("м", "m")})"
            }
            .toList()
        if (levels.isNotEmpty()) {
            add("Высота / эшелон", "Level", levels.joinToString(", "))
        } else if (Regex("\\bSFC\\b").containsMatchIn(normalized)) {
            add("Высота / эшелон", "Level", tr("у земли", "surface"))
        }

        val hazards = linkedSetOf<String>()
        if (Regex("\\bTURB\\b").containsMatchIn(normalized)) {
            hazards += tr("турбулентность", "turbulence")
        }
        if (Regex("\\bICE(?:ING)?\\b").containsMatchIn(normalized)) {
            hazards += tr("обледенение", "icing")
        }
        if (Regex("\\bMTW\\b").containsMatchIn(normalized)) {
            hazards += tr("горные волны", "mountain waves")
        }
        if (Regex("\\bLLWS\\b").containsMatchIn(normalized)) {
            hazards += tr("сдвиг ветра у земли", "low-level wind shear")
        }
        if (Regex("\\bVA\\b").containsMatchIn(normalized)) {
            hazards += tr("вулканический пепел", "volcanic ash")
        }
        if (Regex("\\bTC\\b").containsMatchIn(normalized)) {
            hazards += tr("тропический циклон", "tropical cyclone")
        }
        if (tokens.any { it.startsWith("TS") }) {
            hazards += tr("гроза", "thunderstorm")
        }
        if (hazards.isNotEmpty()) {
            add("Опасные явления", "Hazards", hazards.joinToString(", "))
        }

        val intensity = when {
            Regex("\\bSEV\\b").containsMatchIn(normalized) -> tr("сильная", "severe")
            Regex("\\bMOD\\b").containsMatchIn(normalized) -> tr("умеренная", "moderate")
            else -> null
        }
        if (intensity != null) {
            add("Интенсивность", "Intensity", intensity)
        }

        val frequency = mutableListOf<String>()
        if ("OCNL" in tokens) frequency += tr("отдельные", "occasional")
        if ("FRQ" in tokens) frequency += tr("частые", "frequent")
        if ("ISOL" in tokens) frequency += tr("изолированные", "isolated")
        if ("EMBD" in tokens) frequency += tr("встроенные", "embedded")
        if (frequency.isNotEmpty()) {
            add("Повторяемость", "Occurrence", frequency.joinToString(", "))
        }
    }

    return lines
}

private val EXCLUDED_STATION_WORDS = setOf(
    "METAR", "SPECI", "TAF", "GAMET", "SIGMET", "AIRMET",
    "VALID", "TEMPO", "BECMG", "NOSIG", "CAVOK", "AREA",
    "SFC", "TURB", "ICING", "NSC", "NCD", "SKC", "CLR"
)

private val CLEAR_CLOUD_WORDS = setOf("NSC", "NCD", "SKC", "CLR")

private val WIND_REGEX =
    Regex("\\b(VRB|\\d{3})(\\d{2,3})(G\\d{2,3})?(KT|MPS|KMH)\\b")

private val RVR_REGEX =
    Regex("\\bR(\\d{2}[LRC]?)/([MP]?\\d{4})([UDN]?)\\b")

private val TEMPERATURE_REGEX =
    Regex("\\b(M?\\d{2})/(M?\\d{2})\\b")

private val CLOUD_REGEX =
    Regex("\\b(FEW|SCT|BKN|OVC)(\\d{3})(CB|TCU)?\\b")

private val WEATHER_TOKEN_REGEX =
    Regex("(?<![A-Z])(?:VC|[+-])?(?:TS|SH|FZ|BL|DR|MI|BC|PR|RA|SN|DZ|GR|GS|PL|IC|UP|FG|BR|HZ|FU|VA|DU|SA|SS|DS|FC|SQ|PO|SG|PY)+\\b")

private val WEATHER_CODES = mapOf(
    "DZ" to ("морось" to "drizzle"),
    "RA" to ("дождь" to "rain"),
    "SN" to ("снег" to "snow"),
    "SG" to ("снежные зёрна" to "snow grains"),
    "IC" to ("ледяные кристаллы" to "ice crystals"),
    "PL" to ("ледяные иглы" to "ice pellets"),
    "GR" to ("град" to "hail"),
    "GS" to ("мелкий град" to "small hail"),
    "UP" to ("неизвестные осадки" to "unknown precipitation"),
    "BR" to ("дымка" to "mist"),
    "FG" to ("туман" to "fog"),
    "FU" to ("дым" to "smoke"),
    "VA" to ("вулканический пепел" to "volcanic ash"),
    "DU" to ("пыль" to "dust"),
    "SA" to ("песок" to "sand"),
    "HZ" to ("мгла" to "haze"),
    "PY" to ("водяная пыль" to "spray"),
    "PO" to ("пыльные вихри" to "dust whirls"),
    "SQ" to ("шквал" to "squalls"),
    "FC" to ("воронкообразное облако" to "funnel cloud"),
    "SS" to ("песчаная буря" to "sandstorm"),
    "DS" to ("пыльная буря" to "duststorm"),
    "TS" to ("гроза" to "thunderstorm"),
    "SH" to ("ливень" to "showers"),
    "FZ" to ("замерзающий" to "freezing"),
    "BL" to ("позёмок" to "blowing"),
    "DR" to ("переносимый" to "drifting"),
    "MI" to ("низкий" to "shallow"),
    "BC" to ("отдельный" to "patches"),
    "PR" to ("частичный" to "partial")
)

private val CLOUD_COVERS = mapOf(
    "FEW" to ("незначительная" to "few"),
    "SCT" to ("рассеянная" to "scattered"),
    "BKN" to ("значительная" to "broken"),
    "OVC" to ("сплошная" to "overcast")
)

private fun windUnit(unit: String, english: Boolean): String = when (unit) {
    "KT" -> if (english) "kt" else "уз"
    "MPS" -> if (english) "m/s" else "м/с"
    "KMH" -> if (english) "km/h" else "км/ч"
    else -> unit
}

private fun visibilityText(value: String, english: Boolean): String {
    val meters = value.toIntOrNull() ?: return value
    return when {
        meters == 9999 -> if (english) "10 km or more" else "10 км и более"
        meters >= 1000 -> "${meters / 1000} ${if (english) "km" else "км"}"
        else -> "$meters ${if (english) "m" else "м"}"
    }
}

private fun parseTemperature(value: String): Int? {
    val negative = value.startsWith("M")
    val number = value.removePrefix("M").toIntOrNull() ?: return null
    return if (negative) -number else number
}

private fun formatDayHour(value: String): String {
    if (value.length != 4) return value
    return "${value.substring(0, 2)} ${value.substring(2, 4)}:00"
}

private fun formatValidity(value: String): String = when (value.length) {
    6 -> "${value.substring(0, 2)} ${value.substring(2, 4)}:${value.substring(4, 6)}"
    4 -> formatDayHour(value)
    else -> value
}
