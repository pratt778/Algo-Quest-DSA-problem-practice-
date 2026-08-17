package com.example.algoquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import com.example.algoquest.ui_screen.SettingsScreen
import com.example.algoquest.utils.ThemeManager

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.applyTheme(this)
        setContent {
            val isDarkTheme = ThemeManager.getDarkMode(this@SettingsActivity)
            var darkTheme by remember { mutableStateOf(isDarkTheme) }

            // Save theme when it changes
            LaunchedEffect(darkTheme) {
                ThemeManager.setDarkMode(this@SettingsActivity, darkTheme)
            }

            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                SettingsScreen(
                    currentDarkTheme = darkTheme,
                    onThemeChange = { newTheme -> darkTheme = newTheme },
                    onNavigateBack = { finish() }
                )
            }
        }
    }
}