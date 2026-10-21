package com.yc.navigator.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun appLocale(languageTag: String): Locale =
    if (languageTag == "en") Locale.ENGLISH else Locale.forLanguageTag("ru-RU")

fun formatDate(iso: String?, languageTag: String, long: Boolean = false): String {
    if (iso.isNullOrBlank()) return "—"
    val locale = appLocale(languageTag)
    val pattern = when {
        languageTag == "en" && long -> "d MMM yyyy"
        languageTag == "en" -> "dd.MM.yyyy"
        long -> "d MMMM yyyy"
        else -> "dd.MM.yyyy"
    }
    return runCatching {
        LocalDate.parse(iso).format(DateTimeFormatter.ofPattern(pattern, locale))
    }.getOrDefault(iso)
}

fun formatMinutes(totalMinutes: Int?): String {
    if (totalMinutes == null) return "—"
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return "$hours:${minutes.toString().padStart(2, '0')}"
}

fun formatMinutesShort(totalMinutes: Int?): String =
    if (totalMinutes == null) "—" else "$totalMinutes"

fun parseTimeToMinutes(text: String): Int? {
    val clean = text.trim().replace(',', ':')
    if (clean.isEmpty()) return null
    if (':' in clean) {
        val parts = clean.split(':')
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        if (minutes !in 0..59) return null
        return hours * 60 + minutes
    }
    return clean.toIntOrNull()
}

fun routeSummary(journal: com.yc.navigator.data.Journal): String {
    val departure = journal.departure.name.trim()
    val arrival = journal.arrival.name.trim()
    if (departure.isNotEmpty() || arrival.isNotEmpty()) {
        return listOf(departure.ifEmpty { "—" }, arrival.ifEmpty { "—" }).joinToString(" → ")
    }
    val routes = journal.legs.map { it.route.trim() }.filter { it.isNotEmpty() }
    return when {
        routes.isEmpty() -> "—"
        routes.size == 1 -> routes.first()
        else -> listOf(routes.first(), routes.last()).joinToString(" → ")
    }
}

fun journalTitle(journal: com.yc.navigator.data.Journal, noNumber: String): String =
    journal.number.ifBlank { noNumber }

