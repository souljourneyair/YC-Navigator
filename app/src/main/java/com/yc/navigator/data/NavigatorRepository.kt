package com.yc.navigator.data

import android.util.Log
import java.io.File
import kotlinx.serialization.json.Json

class NavigatorRepository(context: android.content.Context) {

    private val dir = File(context.filesDir, "yc_navigator").apply { mkdirs() }
    private val journalsFile = File(dir, "journals.json")
    private val pilotsFile = File(dir, "pilots.json")

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun loadJournals(): List<Journal> {
        return try {
            readList(journalsFile)
        } catch (e: DataCorruptedException) {
            Log.e("NavigatorRepository", "Journals data corrupted, returning empty list", e)
            emptyList()
        }
    }

    fun loadPilots(): List<Pilot> {
        return try {
            readList(pilotsFile)
        } catch (e: DataCorruptedException) {
            Log.e("NavigatorRepository", "Pilots data corrupted, returning empty list", e)
            emptyList()
        }
    }

    fun saveJournals(journals: List<Journal>) {
        writeText(journalsFile, json.encodeToString(journals))
    }

    fun savePilots(pilots: List<Pilot>) {
        writeText(pilotsFile, json.encodeToString(pilots))
    }

    fun exportBundle(
        journals: List<Journal>,
        pilots: List<Pilot>,
        settings: AppSettings
    ): String = json.encodeToString(
        ExportBundle(
            journals = journals,
            pilots = pilots,
            settings = settings
        )
    )

    fun importBundle(raw: String): ExportBundle {
        try {
            val bundle = json.decodeFromString<ExportBundle>(raw)

            // Validate imported data
            if (bundle.journals.any { it.id <= 0 }) {
                throw ImportException("Invalid journal ID found")
            }
            if (bundle.journals.map { it.id }.toSet().size != bundle.journals.size) {
                throw ImportException("Duplicate journal ID found")
            }
            if (bundle.pilots.any { it.id <= 0 || it.name.isBlank() }) {
                throw ImportException("Invalid pilot data found")
            }
            if (bundle.pilots.map { it.id }.toSet().size != bundle.pilots.size) {
                throw ImportException("Duplicate pilot ID found")
            }
            bundle.journals.forEach { journal ->
                val legIds = journal.legs.map { it.id }
                if (legIds.any { it <= 0 } || legIds.toSet().size != legIds.size) {
                    throw ImportException("Invalid or duplicate leg ID found")
                }
            }

            return bundle
        } catch (e: Exception) {
            when (e) {
                is ImportException -> throw e
                else -> throw ImportException("Failed to parse import data", e)
            }
        }
    }

    private inline fun <reified T> readList(file: File): List<T> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val content = file.readText(Charsets.UTF_8)
            if (content.isBlank()) return emptyList()
            json.decodeFromString<List<T>>(content)
        }.getOrElse { error ->
            Log.e("NavigatorRepository", "Failed to read ${file.name}", error)
            throw DataCorruptedException("Failed to load data from ${file.name}", error)
        }
    }

    private fun writeText(file: File, text: String) {
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(text, Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(text, Charsets.UTF_8)
            tmp.delete()
        }
    }
}
