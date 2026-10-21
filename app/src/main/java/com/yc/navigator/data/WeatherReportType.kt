package com.yc.navigator.data

enum class WeatherReportType(val key: String) {
    METAR_TAF("METAR/TAF"),
    GAMET("GAMET"),
    SIGMET("SIGMET"),
    AIRMET("AIRMET");

    companion object {
        fun fromKey(key: String): WeatherReportType? =
            entries.firstOrNull { it.key == key }
    }
}
