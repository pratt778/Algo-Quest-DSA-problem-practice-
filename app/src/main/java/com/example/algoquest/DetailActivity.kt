package com.example.algoquest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.algoquest.model.Problem
import com.example.algoquest.ui_screen.DetailScreen
import com.example.algoquest.ui.theme.AlgoQuestTheme
import com.example.algoquest.utils.ThemeManager

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val problem = intent.getParcelableExtra<Problem>("problem") ?: run {
            finish()
            return
        }

        setContent {
            val isDarkTheme = ThemeManager.getDarkMode(this@DetailActivity)
            var darkTheme by remember { mutableStateOf(isDarkTheme) }

            LaunchedEffect(darkTheme) {
                ThemeManager.setDarkMode(this@DetailActivity, darkTheme)
            }
            AlgoQuestTheme {
                DetailScreen(
                    problem = problem,
                    onNavigateToProblem = { newProblem ->
                        val intent = Intent(this, DetailActivity::class.java)
                        intent.putExtra("problem", newProblem)
                        startActivity(intent)
                    }
                )
            }
        }
    }
}