package com.example.algoquest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.algoquest.ui.screen.LoginScreen
import com.example.algoquest.ui.theme.AlgoQuestTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlgoQuestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onNavigateToMainActivity = {
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    )
                }
            }
        }
    }
}