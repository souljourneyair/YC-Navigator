package com.yc.navigator.data

import android.content.Context

class IdGenerator(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun nextId(): Long = synchronized(this) {
        val lastId = prefs.getLong(KEY_LAST_ID, System.currentTimeMillis() * 1000)
        val newId = maxOf(lastId + 1, System.currentTimeMillis() * 1000)
        prefs.edit().putLong(KEY_LAST_ID, newId).apply()
        newId
    }

    fun updateMaxId(id: Long) = synchronized(this) {
        val currentMax = prefs.getLong(KEY_LAST_ID, 0L)
        if (id > currentMax) {
            prefs.edit().putLong(KEY_LAST_ID, id).apply()
        }
    }

    companion object {
        private const val PREFS_NAME = "yc_navigator_id_generator"
        private const val KEY_LAST_ID = "last_id"
    }
}
