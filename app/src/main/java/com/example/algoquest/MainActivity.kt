package com.example.algoquest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.algoquest.Auth.AuthManager
import com.example.algoquest.model.Problem
import com.example.algoquest.storage.FirestoreManager
import com.example.algoquest.utils.ProblemUtils
import com.example.algoquest.utils.UserProgress
import com.example.algoquest.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

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
            MaterialTheme {
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
                    }
                )
            }
        }
    }
}
@OptIn(UnstableApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    userEmail: String,
    userId: String,
    onProblemClick: (Problem) -> Unit,
    onLogout: () -> Unit
) {
    var points by remember { mutableStateOf(0L) }
    var isSynced by remember { mutableStateOf(false) }

    val problems by viewModel.problems.observeAsState(emptyList())

    // Fetch user points
    LaunchedEffect(userId) {
        FirestoreManager.getUserData(userId) { data ->
            points = data?.get("points") as? Long ?: 0L
        }
    }

    // Sync user progress
    LaunchedEffect(userId) {
        UserProgress.syncFromFirestore(userId) {
            Log.d("MainActivity", "User progress synced")
            isSynced = true
            viewModel.loadProblems()
        }
    }

    // Process problems
    val displayProblems = remember(problems, isSynced) {
        if (isSynced && problems.isNotEmpty()) {
            try {
                val sortedProblems = ProblemUtils.sortProblemsTopologically(problems)
                ProblemUtils.getUnlockedProblems(sortedProblems)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error processing problems", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        // User Info Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = userEmail,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Points with icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Stars,
                    contentDescription = "Points",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Your Points: $points",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Logout Button (outlined for less visual weight)
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 16.dp),
            shape = MaterialTheme.shapes.small
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Logout")
        }

        // Problems List
        if (displayProblems.isEmpty() && isSynced) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No problems available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(displayProblems) { problem ->
                    ProblemCard(
                        problem = problem,
                        onClick = { onProblemClick(problem) }
                    )
                }
            }
        }
    }
}

@SuppressLint("SuspiciousIndentation")
@OptIn(UnstableApi::class)
@Composable
fun ProblemCard(
    problem: Problem,
    onClick: () -> Unit
) {
    val userId = AuthManager.currentUser()?.uid
    if (userId != null)
        FirestoreManager.getUserData(userId) { data ->
            try {
                val points = data?.get("points") as? Long ?: 0L
                val solvedProblems = data?.get("solvedProblems") as? List<String> ?: emptyList()

                if (problem.id in solvedProblems) {
                    problem.isSolved.value = true
                }
            } catch (e: Exception) {
                androidx.media3.common.util.Log.e("DetailActivity", "Error fetching user data", e)
            }
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // flat but with shadow on tap
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {



            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = problem.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Stars,
                    contentDescription = "Points",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+${problem.points}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )





                if (problem.isSolved.value) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Solved",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }



            // Points badge
            Row(verticalAlignment = Alignment.CenterVertically) {


                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = problem.description.ifEmpty { "No description available." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (problem.prerequisites.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Prerequisites:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    problem.prerequisites.forEach { prereq ->
                        Text(
                            text = "• $prereq",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


