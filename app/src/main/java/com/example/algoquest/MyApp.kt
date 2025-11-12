// File: MyApp.kt
package com.example.algoquest


import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import com.example.algoquest.model.Problem
import com.example.algoquest.viewmodel.MainViewModel

@Composable
fun MyApp(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    viewModel: MainViewModel,
    userEmail: String,
    userId: String,
    onProblemClick: (Problem) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showOnboarding by remember {
        mutableStateOf(!isOnboardingCompleted(context))
    }

    MaterialTheme(
        colorScheme = if (isDarkTheme) MaterialTheme.colorScheme.copy() else MaterialTheme.colorScheme.copy()
        // Note: You're already setting light/dark scheme in MainActivity,
        // so we just reuse it here. Alternatively, pass scheme directly.
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showOnboarding) {
                OnboardingScreen(
                    onFinish = {
                        markOnboardingCompleted(context)
                        showOnboarding = false
                    }
                )
            } else {
                MainScreen(
                    viewModel = viewModel,
                    userEmail = userEmail,
                    userId = userId,
                    onProblemClick = onProblemClick,
                    onLogout = onLogout,
                    darkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle
                )
            }
        }
    }
}

// SharedPreferences helpers
private fun isOnboardingCompleted(context: Context): Boolean {
    return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .getBoolean("onboarding_completed", false)
}

private fun markOnboardingCompleted(context: Context) {
    context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        .edit { putBoolean("onboarding_completed", true) }
}