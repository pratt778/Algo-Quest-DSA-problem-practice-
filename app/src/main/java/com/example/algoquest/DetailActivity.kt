package com.example.algoquest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.algoquest.Auth.AuthManager
import com.example.algoquest.adapter.RecommendationAdapter
import com.example.algoquest.adapter.SuggestionAdapter
import com.example.algoquest.js.JsExecutor
import com.example.algoquest.model.Problem
import com.example.algoquest.storage.FirestoreManager
import com.example.algoquest.utils.RecommendationUtils
import com.example.algoquest.utils.Trie
import com.example.algoquest.viewmodel.MainViewModel
import kotlin.getValue

class DetailActivity : ComponentActivity() {

    private lateinit var jsExecutor: JsExecutor

    private val viewModel: MainViewModel by viewModels()
    private lateinit var progressText: TextView
    private lateinit var runButton: Button
    private lateinit var codeInput: EditText
    private lateinit var outputText: TextView
    private lateinit var hintText: TextView
    private lateinit var problem: Problem

    private lateinit var jsTrie: Trie

    private lateinit var suggestionAdapter: SuggestionAdapter
    private var isSolved = false

    @OptIn(UnstableApi::class)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Initialize JS Trie with keywords
        jsTrie = Trie()
        val jsKeywords = listOf(
            // Variables & keywords
            "var","let","const","function","if","else","for","while",
            "do","switch","case","default","break","continue","return",
            "try","catch","finally","throw","class","extends","super",
            "import","export","from","as","new","delete","typeof","instanceof","in","of","void","yield","await",

            // Values
            "true","false","null","undefined","NaN","Infinity",

            // Objects
            "Object","Array","String","Number","Boolean","Date","Math","RegExp","JSON","Promise","Map","Set","WeakMap","WeakSet",

            // Array methods
            "push","pop","shift","unshift","map","filter","reduce","forEach","find","findIndex","slice","splice","sort","concat","includes","indexOf","join","reverse",

            // String methods
            "charAt","charCodeAt","concat","includes","endsWith","indexOf","lastIndexOf","match","replace","slice","split","startsWith","substring","toLowerCase","toUpperCase","trim",

            // Math methods
            "abs","ceil","floor","round","max","min","pow","sqrt","random","sin","cos","tan","log","exp",

            // Console
            "console","console.log","console.error","console.warn","console.info","console.table",

            // Functions
            "setTimeout","setInterval","clearTimeout","clearInterval","parseInt","parseFloat","isNaN","isFinite","eval","encodeURI","decodeURI","encodeURIComponent","decodeURIComponent"
        )

        jsKeywords.forEach { jsTrie.insert(it) }

        // Retrieve problem from intent
        problem = intent.getParcelableExtra<Problem>("problem") ?: run {
            Log.e("DetailActivity", "No problem data provided")
            Toast.makeText(this, "No problem data provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        jsExecutor = JsExecutor() // Initialize JS executor

        // Initialize UI elements
        progressText = findViewById(R.id.progressText)
        runButton = findViewById(R.id.runButton)
        codeInput = findViewById(R.id.codeInput)
        outputText = findViewById(R.id.outputText)
        hintText = findViewById(R.id.hintText)


        suggestionAdapter = SuggestionAdapter(emptyList()) { suggestion ->
            insertSuggestion(suggestion)
        }

        // Bind problem details
        findViewById<TextView>(R.id.titleText).text = problem.title
        findViewById<TextView>(R.id.descriptionText).text = problem.description
        findViewById<TextView>(R.id.categoryText).text = "Category: ${problem.category}"
        findViewById<TextView>(R.id.pointsText).text = "Points: ${problem.points}"
        findViewById<TextView>(R.id.tagsText).text = "Tags: ${problem.tags.joinToString(", ")}"
        findViewById<TextView>(R.id.hintsText).text = "Hints: ${problem.hints.joinToString(", ")}"

        // Check for logged-in user
        val userId = AuthManager.currentUser()?.uid
        if (userId != null) {
            FirestoreManager.getUserData(userId) { data ->
                try {
                    val points = data?.get("points") as? Long ?: 0L
                    val solvedProblems = data?.get("solvedProblems") as? List<String> ?: emptyList()
                    progressText.text = "Your Points: $points"
                    if (problem.id in solvedProblems) {
                        isSolved = true
                        disableInputs()
                        outputText.text = "✅ Already Solved"
                    }
                } catch (e: Exception) {
                    Log.e("DetailActivity", "Error fetching user data", e)
                    progressText.text = "Your Points: 0"
                    Toast.makeText(this, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            progressText.text = "Your Points: 0 (Log in to track progress)"
        }

        // Run button logic
        runButton.setOnClickListener {
            val userCode = codeInput.text.toString().trim()
            if (userCode.isEmpty()) {
                outputText.text = "Please enter some JavaScript code."
            } else {
                try {
                    val result = jsExecutor.execute(userCode).trim()
                    if (result == problem.answer.trim()) {
                        outputText.text = "✅ Correct! Output: $result"
                        if (userId != null && !isSolved) {
                            disableInputs()
                            FirestoreManager.incrementPoints(userId, problem.points)
                            FirestoreManager.markProblemSolved(userId, problem.id)
                            FirestoreManager.getUserData(userId) { newData ->
                                try {
                                    val newPoints = newData?.get("points") as? Long ?: 0L
                                    progressText.text = "Your Points: $newPoints"
                                } catch (e: Exception) {
                                    Log.e("DetailActivity", "Error updating points", e)
                                    progressText.text = "Your Points: 0"
                                }
                            }
                        }
                    } else {
                        outputText.text = "❌ Wrong! Output: $result\nExpected: ${problem.answer}"
                    }
                } catch (e: Exception) {
                    Log.e("DetailActivity", "Error executing JS code", e)
                    outputText.text = "Error: ${e.message}"
                }
            }
        }

        // Autocomplete text watcher
        codeInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val lastWord = s.toString().split("\\s+".toRegex()).lastOrNull() ?: ""
                val matches = jsTrie.searchPrefix(lastWord)
                if (matches.isNotEmpty()) {
                    hintText.text = s.toString().removeSuffix(lastWord) + matches.first()
                } else {
                    hintText.text = ""
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        codeInput.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_TAB) {
                val hint = hintText.text.toString()
                if (hint.isNotEmpty()) {
                    insertSuggestion(hint.split("\\s+".toRegex()).last())
                    return@setOnKeyListener true
                }
            }
            false
        }

        val recommendRecyclerView = findViewById<RecyclerView>(R.id.recommendRecyclerView)
        recommendRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

// Fetch all problems (from Firestore or local JSON)
        viewModel.problems.observe(this) { allProblems ->
            val recommendations = RecommendationUtils.recommendUsingDijkstra(problem, allProblems)
            val adapter = RecommendationAdapter(recommendations) { selectedProblem ->
                val intent = Intent(this, DetailActivity::class.java)
                intent.putExtra("problem", selectedProblem)
                startActivity(intent)
            }
            recommendRecyclerView.adapter = adapter
        }

        // Trigger loading of problems
        viewModel.loadProblems()




    }

    private fun disableInputs() {
        runButton.isEnabled = false
        codeInput.isEnabled = false
    }

    private fun insertSuggestion(suggestion: String) {
        val text = codeInput.text.toString()
        val tokens = text.split("\\s+".toRegex()).toMutableList()
        if (tokens.isNotEmpty()) {
            tokens[tokens.size - 1] = suggestion
        }
        codeInput.setText(tokens.joinToString(" ") + " ")
        codeInput.setSelection(codeInput.text.length)
        jsTrie.insert(suggestion) // optional: dynamic identifiers
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::jsExecutor.isInitialized) jsExecutor.close()
    }
}
