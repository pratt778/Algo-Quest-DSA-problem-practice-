package com.example.algoquest.ui_screen


import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.algoquest.Auth.AuthManager
import com.example.algoquest.js.JsExecutor
import com.example.algoquest.model.Problem
import com.example.algoquest.storage.FirestoreManager
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

    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Progress
            Text(
                text = progressText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Problem Info
            Text(text = problem.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = problem.description, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(12.dp))

            // Metadata
            MetadataRow(label = "Category", value = problem.category)
            MetadataRow(label = "Points", value = problem.points.toString())
            MetadataRow(label = "Tags", value = problem.tags.joinToString(", "))
            MetadataRow(label = "Hints", value = problem.hints.joinToString(", "))

            Spacer(modifier = Modifier.height(20.dp))

            // Code Editor with Hint Overlay
            // Code Editor with Autocomplete Hint Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                    .padding(12.dp)
            ) {
                // Hint (ghost text) – always behind
                if (hintText.isNotEmpty()) {
                    Text(
                        text = hintText,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
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
                        .height(100.dp),// minor alignment tweak
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onSurface // normal input color
                    ),
                    decorationBox = @Composable { innerTextField ->
                        // No label, no underline – just raw field
                        innerTextField()
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { executeCode() },
                    enabled = isInputEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Run Code")
                }

                // Add these states
                var isGenerating by remember { mutableStateOf(false) }
                val context = LocalContext.current

// Hardcode your API key (for testing only!)
                val GEMINI_API_KEY = "AIzaSyBre60ef7rbpXMA6sTyeQygVTFTlFEoJYI" // ← Replace with your key

// Update the Solve Problem button
                Button(
                    onClick = {
                        if (GEMINI_API_KEY == "YOUR_API_KEY_HERE") {
                            Toast.makeText(context, "Please add your Gemini API key in code", Toast.LENGTH_LONG).show()
                            return@Button
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
                                // Check if it's an auth error
                                outputText = "❌ AI failed: Invalid API key or no internet"
                                Toast.makeText(context, "AI codegen failed. Check API key & internet.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = isInputEnabled && !isGenerating,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGenerating) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text("Solve with AI", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Output
            Text(
                text = "Output:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = outputText,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    .padding(12.dp),
                color = if (outputText.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recommendations
            Text(
                text = "Recommended Problems",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (recommendations.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    recommendations.forEach { recProblem ->
                        RecommendationCard(
                            problem = recProblem,
                            onClick = { onNavigateToProblem(recProblem) }
                        )
                    }
                }
            } else {
                Text("No recommendations available", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label:", fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecommendationCard(problem: Problem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(problem.title, fontWeight = FontWeight.Medium, maxLines = 2)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${problem.points} pts • ${problem.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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