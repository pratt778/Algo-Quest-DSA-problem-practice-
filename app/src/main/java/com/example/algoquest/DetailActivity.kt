package com.example.algoquest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.algoquest.model.Problem
import com.example.algoquest.ui_screen.DetailScreen
import com.example.algoquest.ui.theme.AlgoQuestTheme

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val problem = intent.getParcelableExtra<Problem>("problem") ?: run {
            finish()
            return
        }

        setContent {
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