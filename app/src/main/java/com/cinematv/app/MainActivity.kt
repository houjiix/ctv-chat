package com.cinematv.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cinematv.app.ui.CinemaApp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as CinemaApplication
        lifecycleScope.launch {
            val language = app.settings.preferences.first().language
            applyLanguage(language)
        }

        setContent { CinemaApp() }
    }

    private fun applyLanguage(language: String) {
        val tags = when (language) {
            "en" -> "en"
            "fr" -> "fr"
            "ar" -> "ar"
            else -> ""
        }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
    }
}
