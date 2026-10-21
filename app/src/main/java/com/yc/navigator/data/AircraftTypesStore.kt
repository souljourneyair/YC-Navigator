package com.yc.navigator.data

import android.content.Context
import kotlinx.serialization.json.Json

class AircraftTypesStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json

    fun isInitialized(): Boolean = prefs.contains(KEY_TYPES)

    fun load(): List<String> {
        val raw = prefs.getString(KEY_TYPES, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<String>>(raw)
        }.getOrDefault(emptyList())
    }

    fun save(types: List<String>) {
        prefs.edit()
            .putString(KEY_TYPES, json.encodeToString(types))
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "yc_navigator_aircraft_types"
        private const val KEY_TYPES = "types"
    }
}
