// SettingsScreen.kt
package com.example.algoquest.ui_screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(
    currentDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit
) {
    var darkTheme by remember { mutableStateOf(currentDarkTheme) }

    // Update parent when theme changes
    LaunchedEffect(darkTheme) {
        onThemeChange(darkTheme)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (darkTheme) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FF),
                            Color(0xFFFFFFFF)
                        )
                    )
                }
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom App Bar with gradient
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF667EEA),
                                    Color(0xFF764BA2)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Settings Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                // Appearance Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkTheme) Color(0xFF1E1E1E) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0xFF667EEA),
                                                Color(0xFF764BA2)
                                            )
                                        ),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Appearance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (darkTheme) Color.White else Color(0xFF1A1A1A)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Dark mode toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (darkTheme) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)
                                )
                                .clickable { darkTheme = !darkTheme }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Dark theme",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (darkTheme) Color.White else Color(0xFF1A1A1A)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (darkTheme) "Enabled" else "Disabled",
                                    color = if (darkTheme) Color(0xFF667EEA) else Color(0xFF667EEA),
                                    fontSize = 14.sp
                                )
                            }
                            Switch(
                                checked = darkTheme,
                                onCheckedChange = { darkTheme = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF667EEA),
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (darkTheme)
                            Color(0xFF1E1E1E)
                        else
                            Color(0xFFF8F9FF)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF667EEA))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Theme follows system by default. Toggle to override.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (darkTheme)
                                Color.White.copy(alpha = 0.7f)
                            else
                                Color(0xFF757575),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// UPDATED MainActivity.kt
// ==========================================

/*
Update your MainActivity.kt onCreate like this:

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirebaseApp.initializeApp(this)

    val currentUser = AuthManager.currentUser()
    if (currentUser == null) {
        startActivity(Intent(this, SignUpActivity::class.java))
        finish()
        return
    }

    setContent {
        var darkTheme by remember { mutableStateOf(false) }
        
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
        ) {
            MainScreen(
                viewModel = viewModel,
                userEmail = currentUser.email ?: "",
                userId = currentUser.uid,
                onProblemClick = { problem ->
                    val intent = Intent(this, DetailActivity::class.java)
                    intent.putExtra("problem", problem)
                    startActivity(intent)
                },
                onLogout = {
                    AuthManager.signOut()
                    startActivity(Intent(this, SignUpActivity::class.java))
                    finish()
                },
                darkTheme = darkTheme,
                onThemeToggle = { newTheme -> darkTheme = newTheme }
            )
        }
    }
}
*/

// ==========================================
// UPDATED SettingsActivity.kt
// ==========================================

/*
Create or update SettingsActivity.kt:

package com.example.algoquest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import com.example.algoquest.ui_screen.SettingsScreen

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var darkTheme by remember { mutableStateOf(false) }
            
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
*/