package com.yc.navigator.data

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        languageTag = prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE,
        unitMode = runCatching {
            UnitMode.valueOf(prefs.getString(KEY_UNITS, UnitMode.AUTO.name) ?: UnitMode.AUTO.name)
        }.getOrDefault(UnitMode.AUTO),
        themeMode = runCatching {
            ThemeMode.valueOf(
                prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            )
        }.getOrDefault(ThemeMode.SYSTEM)
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_LANGUAGE, settings.languageTag)
            .putString(KEY_UNITS, settings.unitMode.name)
            .putString(KEY_THEME, settings.themeMode.name)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "yc_navigator_settings"
        const val KEY_LANGUAGE = "language"
        const val KEY_UNITS = "units"
        const val KEY_THEME = "theme"
        const val DEFAULT_LANGUAGE = "ru"
    }
}
