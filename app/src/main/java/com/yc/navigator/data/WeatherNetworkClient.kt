package com.yc.navigator.data

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class AviationWeatherReport(
    val rawOb: String = "",
    val rawTaf: String = ""
)

object WeatherNetworkClient {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchMetarTaf(station: String): String? = withContext(Dispatchers.IO) {
        val icao = station.trim().uppercase(Locale.US)
        require(icao.isNotBlank()) { "Station is empty" }
        val encodedStation = URLEncoder.encode(icao, Charsets.UTF_8.name())
        val url = "https://aviationweather.gov/api/data/metar" +
            "?ids=$encodedStation&format=json&taf=true&hours=2"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            readTimeout = TimeUnit.SECONDS.toMillis(20).toInt()
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "YC-Navigator-Weather")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Weather request failed: HTTP ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                it.readText()
            }
            val reports = json.decodeFromString<List<AviationWeatherReport>>(body)
            val metar = reports.firstOrNull { it.rawOb.isNotBlank() }?.rawOb?.trim()
            val taf = reports.firstOrNull { it.rawTaf.isNotBlank() }?.rawTaf?.trim()
            listOfNotNull(metar, taf)
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .takeIf { it.isNotBlank() }
        } finally {
            connection.disconnect()
        }
    }
}
