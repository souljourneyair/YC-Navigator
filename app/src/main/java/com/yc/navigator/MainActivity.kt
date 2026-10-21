package com.yc.navigator

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.yc.navigator.data.SettingsStore
import com.yc.navigator.ui.AppSplashScreen
import com.yc.navigator.ui.PilotApp
import kotlinx.coroutines.delay
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val languageTag = SettingsStore(newBase).load().languageTag
        val locale = Locale.forLanguageTag(if (languageTag == "en") "en" else "ru-RU")
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                delay(1400)
                showSplash = false
            }

            if (showSplash) {
                AppSplashScreen()
            } else {
                PilotApp()
            }
        }
    }
}
