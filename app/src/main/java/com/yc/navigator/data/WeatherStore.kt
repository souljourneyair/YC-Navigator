package com.yc.navigator.data

import android.content.Context
import kotlinx.serialization.json.Json

class WeatherStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json

    fun load(): Map<String, String> {
        val raw = prefs.getString(KEY_REPORTS, null) ?: return emptyMap()
        val stored = runCatching {
            json.decodeFromString<Map<String, String>>(raw)
        }.getOrDefault(emptyMap())
        if (stored.containsKey(WeatherReportType.METAR_TAF.key)) return stored

        val legacy = listOf(
            stored[WeatherReportType.METAR_TAF.key],
            stored["METAR"],
            stored["TAF"]
        ).filterNotNull()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        return if (legacy.isBlank()) {
            stored
        } else {
            stored + (WeatherReportType.METAR_TAF.key to legacy)
        }
    }

    fun save(reports: Map<String, String>) {
        prefs.edit()
            .putString(KEY_REPORTS, json.encodeToString(reports))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "yc_navigator_weather"
        private const val KEY_REPORTS = "reports"
    }
}
