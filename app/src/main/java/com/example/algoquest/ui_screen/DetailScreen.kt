package com.example.algoquest.ui_screen


import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.algoquest.Auth.AuthManager
import com.example.algoquest.js.JsExecutor
import com.example.algoquest.model.Problem
import com.example.algoquest.storage.FirestoreManager
import com.example.algoquest.ui.components.YesNoDialog
import com.example.algoquest.ui.theme.AlgoQuestTheme
import com.example.algoquest.utils.RecommendationUtils
import com.example.algoquest.utils.Trie
import com.example.algoquest.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
@OptIn(UnstableApi::class)

@Composable
fun DetailScreen(
    problem: Problem,
    onNavigateToProblem: (Problem) -> Unit,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainViewModel: MainViewModel = viewModel()

    var userCode by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("Output will appear here") }
    var hintText by remember { mutableStateOf("") }
    var progressText by remember { mutableStateOf("Your Points: 0") }
    var isSolved by remember { mutableStateOf(false) }
    var isInputEnabled by remember { mutableStateOf(true) }
    var recommendations by remember { mutableStateOf<List<Problem>>(emptyList()) }

    val jsExecutor = remember { JsExecutor() }
    val jsTrie = remember { buildJsTrie() }
    var showAIDialog by remember { mutableStateOf(false) }
    var savedSolution by remember { mutableStateOf<String?>(null) }
    val userId = AuthManager.currentUser()?.uid

    // Load user data & check solved status
    LaunchedEffect(userId) {
        if (userId != null) {
            FirestoreManager.getUserData(userId) { data ->
                try {
                    val points = data?.get("points") as? Long ?: 0L
                    val solvedProblems = data?.get("solvedProblems") as? List<String> ?: emptyList()
                    progressText = "Your Points: $points"
                    if (problem.id in solvedProblems) {
                        isSolved = true
                        isInputEnabled = false
                        outputText = "✅ Already Solved"
                    }
                } catch (e: Exception) {
                    Log.e("DetailScreen", "Error fetching user data", e)
                    progressText = "Your Points: 0"
                    Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            progressText = "Your Points: 0 (Log in to track progress)"
        }
    }

    // Load saved solution if problem is already solved
    LaunchedEffect(userId, problem.id, isSolved) {
        if (userId != null && isSolved) {
            FirestoreManager.getProblemSolution(userId, problem.id) { solution ->
                savedSolution = solution
            }
        }
    }

    // Load recommendations
    LaunchedEffect(mainViewModel.problems) {
        mainViewModel.problems.observeForever { allProblems ->
            recommendations = RecommendationUtils.recommendUsingDijkstra(problem, allProblems)
        }
        mainViewModel.loadProblems()
    }

    // Update hint on code change
    LaunchedEffect(userCode) {
        val lastWord = userCode.split("\\s+".toRegex()).lastOrNull() ?: ""
        val matches = jsTrie.searchPrefix(lastWord)
        hintText = if (matches.isNotEmpty()) {
            val prefix = userCode.substringBeforeLast(lastWord)
            prefix + matches.first()
        } else {
            ""
        }
    }

    fun executeCode() {
        if (userCode.trim().isEmpty()) {
            outputText = "Please enter some JavaScript code."
            return
        }

        try {
            val result = jsExecutor.execute(userCode).trim()
            if (result == problem.answer.trim()) {
                outputText = "✅ Correct! Output: $result"
                if (userId != null && !isSolved) {
                    isInputEnabled = false


                    FirestoreManager.saveProblemSolution(userId, problem.id, userCode) { success ->
                        if (success) {
                            savedSolution = userCode // Update UI immediately
                            Log.d("DetailScreen", "Solution saved successfully")
                        }
                    }

                    FirestoreManager.incrementPoints(userId, problem.points) { success ->
                        if (success) {
                            FirestoreManager.markProblemSolved(userId, problem.id) { solvedSuccess ->
                                if (solvedSuccess) {
                                    FirestoreManager.getUserData(userId) { newData ->
                                        try {
                                            val newPoints = newData?.get("points") as? Long ?: 0L
                                            progressText = "Your Points: $newPoints"
                                        } catch (e: Exception) {
                                            progressText = "Your Points: 0"
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                outputText = "❌ Wrong! Output: $result\nExpected: ${problem.answer}"
            }
        } catch (e: Exception) {
            Log.e("DetailScreen", "Error executing JS code", e)
            outputText = "Error: ${e.message}"
        }
    }

    fun insertSuggestion(suggestion: String) {
        val tokens = userCode.split("\\s+".toRegex()).toMutableList()
        if (tokens.isNotEmpty()) {
            tokens[tokens.size - 1] = suggestion
            userCode = tokens.joinToString(" ") + " "
            jsTrie.insert(suggestion)
        }
    }

    LaunchedEffect(Unit) {
        // Handle Tab key is not possible in Compose EditText directly
        // But we simulate via "autocomplete apply" on hint click or via button
    }

    Box(
        modifier = modifier
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Points Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
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
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Points",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Your Progress",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = progressText.substringAfter(": "),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Problem Title Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Title with colored accent
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(32.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                                    ),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = problem.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = problem.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF424242),
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Metadata Grid
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetadataRow(label = "Category", value = problem.category)
                        MetadataRow(label = "Points", value = problem.points.toString())
                        MetadataRow(label = "Tags", value = problem.tags.joinToString(", "))
                        MetadataRow(label = "Hints", value = problem.hints.joinToString(", "))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Code Editor Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Editor Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Code",
                            tint = Color(0xFF667EEA),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "JavaScript Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isSolved) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Solved",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Solved",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Code Input with Hint
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E1E))
                            .padding(16.dp)
                    ) {
                        // Hint (ghost text) – always behind
                        if (hintText.isNotEmpty()) {
                            Text(
                                text = hintText,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF6A6A6A),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Actual user input (transparent, on top)
                        BasicTextField(
                            value = userCode,
                            onValueChange = { userCode = it },
                            enabled = isInputEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .height(120.dp),
                            textStyle = LocalTextStyle.current.copy(
                                color = Color(0xFFE0E0E0),
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            decorationBox = @Composable { innerTextField ->
                                innerTextField()
                            }
                        )
                    }

                    // Right after the code input Box closes and before Spacer(modifier = Modifier.height(16.dp))

// Display saved solution if available
                    if (savedSolution != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF2A2A2A))
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Saved Solution",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Your Saved Solution:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF4CAF50),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = savedSolution ?: "",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFB0B0B0),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        var isGenerating by remember { mutableStateOf(false) }
                        val context = LocalContext.current
                        val GEMINI_API_KEY = "AIzaSyBre60ef7rbpXMA6sTyeQygVTFTlFEoJYI"

                        Button(
                            onClick = { executeCode() },
                            enabled = isInputEnabled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF667EEA),
                                disabledContainerColor = Color(0xFFE0E0E0)
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            Text(
                                "Run Code",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Button(
                            onClick = {showAIDialog = true},
                            enabled = isInputEnabled && !isGenerating,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF9C27B0),
                                disabledContainerColor = Color(0xFFE0E0E0)
                            ),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    "Solve with AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                        if (showAIDialog) {
                            YesNoDialog(
                                title = "Solve with AI",
                                message = "Do you want to use AI for help?",
                                yesText = "Yep",
                                noText = "Not Now",
                                onYes = {

                                    if (GEMINI_API_KEY == "YOUR_API_KEY_HERE") {
                                        Toast.makeText(context, "Please add your Gemini API key in code", Toast.LENGTH_LONG).show()
//                                            return@Button
                                    }

                                    isGenerating = true
                                    scope.launch {
                                        val generatedCode = generateCodeWithGemini(
                                            problemTitle = problem.title,
                                            problemDescription = problem.description,
                                            apiKey = GEMINI_API_KEY
                                        )

                                        isGenerating = false
                                        if (generatedCode != null) {
                                            userCode = generatedCode
                                            outputText = "💡 AI-generated code inserted. Click 'Run Code' to test!"
                                        } else {
                                            outputText = "❌ AI failed: Invalid API key or no internet"
                                            Toast.makeText(context, "AI codegen failed. Check API key & internet.", Toast.LENGTH_LONG).show()
                                        }
                                    }

                                },
                                onNo = { },
                                onDismiss = { showAIDialog = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Output Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (outputText.startsWith("✅")) Color(0xFFE8F5E9)
                    else if (outputText.startsWith("❌")) Color(0xFFFFEBEE)
                    else Color.White
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        outputText.startsWith("✅") -> Color(0xFF4CAF50)
                                        outputText.startsWith("❌") -> Color(0xFFF44336)
                                        else -> Color(0xFF2196F3)
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Output",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    outputText.startsWith("✅") -> Color(0xFFC8E6C9)
                                    outputText.startsWith("❌") -> Color(0xFFFFCDD2)
                                    else -> Color(0xFFF5F5F5)
                                }
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = outputText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1A1A1A),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Recommendations Section
            if (recommendations.isNotEmpty()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF667EEA))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recommended Problems",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        recommendations.forEach { recProblem ->
                            RecommendationCard(
                                problem = recProblem,
                                onClick = { onNavigateToProblem(recProblem) }
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Color(0xFFE0E0E0)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No recommendations available",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFF9E9E9E)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            jsExecutor.close()
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "$label:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(90.dp),
            color = Color(0xFF667EEA),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            value,
            color = Color(0xFF424242),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable

private fun RecommendationCard(problem: Problem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(280.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp))
            .clickable(
                onClick = onClick,
//                indication = rememberRipple(bounded = true),
                interactionSource = remember { MutableInteractionSource() }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Gradient Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                        )
                    )
            )

            Column(modifier = Modifier.padding(18.dp)) {
                // Title
                Text(
                    text = problem.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                Text(
                    text = problem.description.ifEmpty { "No description available." },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags (Scrollable Row)
                if (problem.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        items(problem.tags) { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF667EEA).copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
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
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Points + Difficulty Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Points Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF8E1))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
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

                    // Difficulty
                    Text(
                        text = problem.category.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = when (problem.category.lowercase()) {
                            "easy" -> Color(0xFF4CAF50)
                            "medium" -> Color(0xFFFF9800)
                            "hard" -> Color(0xFFF44336)
                            else -> Color(0xFF757575)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
// Helper: Build JS Trie once

fun buildJsTrie(): Trie {
    val trie = Trie()
    val jsKeywords = listOf(
        "var","let","const","function","if","else","for","while",
        "do","switch","case","default","break","continue","return",
        "try","catch","finally","throw","class","extends","super",
        "import","export","from","as","new","delete","typeof","instanceof","in","of","void","yield","await",
        "true","false","null","undefined","NaN","Infinity",
        "Object","Array","String","Number","Boolean","Date","Math","RegExp","JSON","Promise","Map","Set","WeakMap","WeakSet",
        "push","pop","shift","unshift","map","filter","reduce","forEach","find","findIndex","slice","splice","sort","concat","includes","indexOf","join","reverse",
        "charAt","charCodeAt","concat","includes","endsWith","indexOf","lastIndexOf","match","replace","slice","split","startsWith","substring","toLowerCase","toUpperCase","trim",
        "abs","ceil","floor","round","max","min","pow","sqrt","random","sin","cos","tan","log","exp",
        "console","console.log","console.error","console.warn","console.info","console.table",
        "setTimeout","setInterval","clearTimeout","clearInterval","parseInt","parseFloat","isNaN","isFinite","eval","encodeURI","decodeURI","encodeURIComponent","decodeURIComponent"
    )
    jsKeywords.forEach { trie.insert(it) }
    return trie
}


@OptIn(UnstableApi::class)
suspend fun generateCodeWithGemini(
    problemTitle: String,
    problemDescription: String,
    apiKey: String
): String? = withContext(Dispatchers.IO) {
    return@withContext try {
        val url = URL("https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val prompt = """
            You are an expert JavaScript coding assistant.
            Solve the following problem in JavaScript.
            Return ONLY the JavaScript code—no explanation, no markdown, no comments.
            if you made a function make sure to call function as well and print answer.
            Dont give multiple answers.
            Problem Title: $problemTitle
            Description: $problemDescription
            
        """.trimIndent()

        val jsonInput = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", prompt)
                }))
            }))
        }

        connection.outputStream.use { os ->
            os.write(jsonInput.toString().toByteArray())
        }

        val response = connection.inputStream?.bufferedReader()?.readText()
        connection.disconnect()

        if (response != null) {
            val jsonResponse = JSONObject(response)
            val candidate = jsonResponse.getJSONArray("candidates").getJSONObject(0)
            val content = candidate.getJSONObject("content")
            val part = content.getJSONArray("parts").getJSONObject(0)
            part.getString("text").trim()
        } else {
            null
        }
    } catch (e: Exception) {
        Log.e("GeminiAPI", "Failed to generate code", e)
        null
    }
}