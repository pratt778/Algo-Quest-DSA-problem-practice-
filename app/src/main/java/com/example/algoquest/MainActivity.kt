package com.example.algoquest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Accessible
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.More
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.util.UnstableApi
import com.example.algoquest.Auth.AuthManager
import com.example.algoquest.Auth.AuthManager.currentUser
import com.example.algoquest.model.Problem
import com.example.algoquest.storage.FirestoreManager
import com.example.algoquest.ui.components.YesNoDialog
import com.example.algoquest.utils.FuzzyMatcher
import com.example.algoquest.utils.ProblemUtils
import com.example.algoquest.utils.SmartShuffle
import com.example.algoquest.utils.ThemeManager
import com.example.algoquest.utils.UserProgress
import com.example.algoquest.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen();
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        ThemeManager.applyTheme(this)
        val currentUser = AuthManager.currentUser()
        if (currentUser == null) {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
            return
        }

        setContent {
            val isDarkTheme = ThemeManager.getDarkMode(this@MainActivity)
            var darkTheme by remember { mutableStateOf(isDarkTheme) }
            LaunchedEffect(darkTheme) {
                ThemeManager.setDarkMode(this@MainActivity, darkTheme)
            }
            MaterialTheme(
                colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            ) {
                MyApp(
                    isDarkTheme = darkTheme,
                    onThemeToggle = { newTheme -> darkTheme = newTheme },
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
//                MainScreen(
//                    viewModel = viewModel,
//                    userEmail = currentUser.email ?: "",
//                    userId = currentUser.uid,
//                    onProblemClick = { problem ->
//                        val intent = Intent(this, DetailActivity::class.java)
//                        intent.putExtra("problem", problem)
//                        startActivity(intent)
//                    },
//                    onLogout = {
//                        AuthManager.signOut()
//                        startActivity(Intent(this, SignUpActivity::class.java))
//                        finish()
//                    },
//                    darkTheme = darkTheme,
//                    onThemeToggle = { newTheme -> darkTheme = newTheme }
//
//                )
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
    onLogout: () -> Unit,
    darkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit // 👈 add this
) {
    val points by viewModel.points.observeAsState(0L)
    var isSynced by remember { mutableStateOf(false) }

    val problems by viewModel.problems.observeAsState(emptyList())

    var selectedDifficulty by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var showOnlySolved by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    val allTags = remember(problems) {
        problems.flatMap { it.tags }
            .distinct()
            .sorted()
    }

    LaunchedEffect(userId) {
        UserProgress.syncFromFirestore(userId) {
            Log.d("MainActivity", "User progress synced")
            isSynced = true
            viewModel.loadProblems()
        }
        viewModel.observeUserPoints(userId)
    }

    BackHandler {
        showExitDialog = true
    }

//    val displayProblems = remember(problems, isSynced, selectedDifficulty, selectedTag, showOnlySolved, searchQuery) {
//        if (!isSynced || problems.isEmpty()) {
//            emptyList()
//        } else {
//            var filtered = problems.toMutableList()
//
//            if (searchQuery.isNotBlank()) {
//                filtered = filtered.filter { FuzzyMatcher.matchesProblem(searchQuery, it) }.toMutableList()
//            }
//
//            selectedDifficulty?.let {
//                filtered = filtered.filter { problem ->
//                    problem.category.trim().lowercase() == selectedDifficulty!!.lowercase()
//                }.toMutableList()
//            }
//
//            if (selectedTag != null) {
//                filtered = filtered.filter { it.tags.contains(selectedTag) }.toMutableList()
//            }
//
//            if (showOnlySolved) {
//                filtered = filtered.filter { it.isSolved.value }.toMutableList()
//            }
//
//            try {
//                val sorted = ProblemUtils.sortProblemsTopologically(filtered)
//                ProblemUtils.getUnlockedProblems(sorted)
//            } catch (e: Exception) {
//                Log.e("MainActivity", "Error processing problems", e)
//                emptyList()
//            }
//        }
//    }
    val userPoints by viewModel.points.observeAsState(0L)
    val recentlyShown by viewModel.recentlyShown.observeAsState(emptySet())

    val shuffledProblems = remember(
        problems, isSynced, selectedDifficulty, selectedTag, showOnlySolved, searchQuery,
        userPoints,
    ) {
        if (!isSynced || problems.isEmpty()) return@remember emptyList()

        // Step 1: Apply filters (search, difficulty, tags, solved)
        var filtered = problems.toMutableList()

        if (searchQuery.isNotBlank()) {
            filtered =
                filtered.filter { FuzzyMatcher.matchesProblem(searchQuery, it) }.toMutableList()
        }
        selectedDifficulty?.let {
            filtered = filtered.filter { p -> p.category.trim().lowercase() == it.lowercase() }
                .toMutableList()
        }
        selectedTag?.let {
            filtered = filtered.filter { p -> p.tags.contains(it) }.toMutableList()
        }
        if (showOnlySolved) {
            filtered = filtered.filter { it.isSolved.value }.toMutableList()
        }

        // Step 2: Topological sort + unlock prerequisites
        val sorted = try {
            ProblemUtils.sortProblemsTopologically(filtered)
        } catch (e: Exception) {
            Log.e("MainScreen", "Topo sort failed", e)
            filtered
        }
        val unlocked = ProblemUtils.getUnlockedProblems(sorted)

        // Step 3: SMART SHUFFLE
        SmartShuffle.shuffle(
            problems = unlocked,
            userPoints = userPoints,
            recentlyShown = recentlyShown?:emptySet(),
            maxResults = 50
        )
    }

// === UPDATE shown problems when list changes ===
    // === UPDATE shown problems when list changes (but avoid infinite loop) ===
    LaunchedEffect(problems, selectedDifficulty, selectedTag, showOnlySolved, searchQuery) {
        kotlinx.coroutines.delay(300)
        if (shuffledProblems.isNotEmpty()) {
            val ids = shuffledProblems.take(15).map { it.id }.toSet()
            viewModel.updateRecentlyShown(ids)
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FF),
                        Color(0xFFFFFFFF)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // User Info Header with gradient card
//            IconButton(
//                onClick = { viewModel.clearRecentlyShown() },
////                modifier = Modifier.align(Alignment.TopEnd)
//            ) {
//                Icon(Icons.Default.Refresh, "Refresh feed", tint = Color.Red)
//            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
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
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Welcome back!",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = currentUser()?.displayName ?: userEmail,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Points with modern badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC107)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Stars,
                                    contentDescription = "Points",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Your Points",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$points",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }


                    val context = LocalContext.current

// Then in button:

                    // Logout button in header
                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (showLogoutDialog) {
                        YesNoDialog(
                            title = "Logout",
                            message = "Do you want to logout?",
                            yesText = "Logout",
                            noText = "Cancel",
                            onYes = onLogout,
                            onDismiss = { showLogoutDialog = false },
                            onNo = { /* just dismiss */ }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    IconButton(
                        onClick = {
                            context.startActivity(Intent(context, SettingsActivity::class.java))
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.More,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                }
            }

            // Modern Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search problems...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF667EEA),
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                textStyle = MaterialTheme.typography.bodyLarge,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color(0xFF667EEA)
                            )
                        }
                    }
                }
            )

            // Difficulty Filter Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    text = "Easy",
                    isSelected = selectedDifficulty == "easy",
                    onClick = { selectedDifficulty = if (selectedDifficulty == "easy") null else "easy" },
                    color = Color(0xFF4CAF50)
                )
                FilterChip(
                    text = "Medium",
                    isSelected = selectedDifficulty == "medium",
                    onClick = { selectedDifficulty = if (selectedDifficulty == "medium") null else "medium" },
                    color = Color(0xFFFF9800)
                )
                FilterChip(
                    text = "Hard",
                    isSelected = selectedDifficulty == "hard",
                    onClick = { selectedDifficulty = if (selectedDifficulty == "hard") null else "hard" },
                    color = Color(0xFFF44336)
                )
                FilterChip(
                    text = if (showOnlySolved) "Solved" else "All",
                    isSelected = showOnlySolved,
                    onClick = { showOnlySolved = !showOnlySolved },
                    color = Color(0xFF9C27B0)
                )
            }

            // Tag Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                item {
                    TagChip(
                        text = "All Tags",
                        isSelected = selectedTag == null,
                        onClick = { selectedTag = null }
                    )
                }
                items(allTags) { tag ->
                    TagChip(
                        text = tag,
                        isSelected = selectedTag == tag,
                        onClick = { selectedTag = if (selectedTag == tag) null else tag }
                    )
                }
            }

            // Problems List
            if (shuffledProblems.isEmpty() && isSynced) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Stars,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFFE0E0E0)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No problems available",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF9E9E9E),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(shuffledProblems) { problem ->
                        ProblemCard(
                            problem = problem,
                            onClick = { onProblemClick(problem) }
                        )
                    }
                }

                // Exit confirmation dialog
                if (showExitDialog) {
                    val context = LocalContext.current as? ComponentActivity

                    YesNoDialog(
                        title = "Exit App",
                        message = "Do you want to exit the app?",
                        yesText = "Exit",
                        noText = "Cancel",
                        onYes = {
                            context?.finish()
                        },
                        onNo = {
                            showExitDialog = false
                        },
                        onDismiss = {
                            showExitDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) color else Color(0xFFF5F5F5)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFF757575)
        )
    }
}

@Composable
fun TagChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected)
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                    )
                else
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFF5F5F5), Color(0xFFF5F5F5))
                    )
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else Color(0xFF757575)
        )
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

    val categoryColor = when (problem.category.lowercase()) {
        "easy" -> Color(0xFF4CAF50)
        "medium" -> Color(0xFFFF9800)
        "hard" -> Color(0xFFF44336)
        else -> Color(0xFF667EEA)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Top colored bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(categoryColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Difficulty Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(categoryColor.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = problem.category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = categoryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Points Badge
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF8E1))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Stars,
                                contentDescription = "Points",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+${problem.points}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFF57C00),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (problem.isSolved.value) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF4CAF50),
                                            Color(0xFF8BC34A)
                                        )
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "✓ Solved",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Description
                Text(
                    text = problem.description.ifEmpty { "No description available." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF424242),
                    lineHeight = 22.sp
                )
                /* ---------- NEW: Tags Row ---------- */
                if (problem.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        items(problem.tags) { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF667EEA).copy(alpha = 0.15f),
                                                Color(0xFF764BA2).copy(alpha = 0.15f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF667EEA),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                if (problem.prerequisites.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Prerequisites:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF667EEA)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            problem.prerequisites.forEach { prereq ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF667EEA))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = prereq,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF616161)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}