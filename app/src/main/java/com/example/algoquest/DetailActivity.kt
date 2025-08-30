package com.example.algoquest

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.example.algoquest.Auth.AuthManager
import com.example.algoquest.js.JsExecutor
import com.example.algoquest.model.Problem
import com.example.algoquest.storage.FirestoreManager

class DetailActivity : ComponentActivity() {

    private lateinit var jsExecutor: JsExecutor
    private lateinit var progressText: TextView
    private lateinit var runButton: Button
    private lateinit var codeInput: EditText
    private lateinit var outputText: TextView
    private lateinit var problem: Problem
    private var isSolved = false

    @OptIn(UnstableApi::class)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Retrieve problem from intent

        jsExecutor = JsExecutor() // Initialize early

        problem = intent.getParcelableExtra<Problem>("problem") ?: run {
            finish()
            return
        }

        // Initialize UI elements
        progressText = findViewById(R.id.progressText)
        runButton = findViewById(R.id.runButton)
        codeInput = findViewById(R.id.codeInput)
        outputText = findViewById(R.id.outputText)

        // Bind problem details to UI
        findViewById<TextView>(R.id.titleText).text = problem.title
        findViewById<TextView>(R.id.descriptionText).text = problem.description
        findViewById<TextView>(R.id.categoryText).text = "Category: ${problem.category}"
        findViewById<TextView>(R.id.pointsText).text = "Points: ${problem.points}"
        findViewById<TextView>(R.id.tagsText).text = "Tags: ${problem.tags.joinToString(", ")}"
        findViewById<TextView>(R.id.hintsText).text = "Hints: ${problem.hints.joinToString(", ")}"

        // Initialize JavaScript executor

        // Check for logged-in user
        val userId = AuthManager.currentUser()?.uid
        if (userId != null) {
            // Fetch user progress from Firestore if logged in
            FirestoreManager.getUserData(userId) { data ->
                val points = data?.get("points") as? Long ?: 0L
                val solvedProblems = data?.get("solvedProblems") as? List<String> ?: emptyList()

                progressText.text = "Your Points: $points"

                if (problem.id in solvedProblems) {
                    isSolved = true
                    disableInputs()
                    outputText.text = "✅ Already Solved"
                }
            }
        } else {
            // Display message for non-logged-in users
            progressText.text = "Your Points: 0 (Log in to track progress)"
        }

        // Set up run button click listener
        runButton.setOnClickListener {
            Log.d("DetailActivity", "Run button clicked")
            val userCode = codeInput.text.toString().trim()
            if (userCode.isEmpty()) {
                outputText.text = "Please enter some JavaScript code."
            } else {
                val result = jsExecutor.execute(userCode).trim()
                if (result == problem.answer.trim()) {
                    outputText.text = "✅ Correct! Output: $result"
                    if (userId != null && !isSolved) {
                        // Update Firestore only if logged in and problem not already solved
                        disableInputs()
                        FirestoreManager.incrementPoints(userId, problem.points)
                        FirestoreManager.markProblemSolved(userId, problem.id)

                        // Update UI points
                        FirestoreManager.getUserData(userId) { newData ->
                            val newPoints = newData?.get("points") as? Long ?: 0L
                            progressText.text = "Your Points: $newPoints"
                        }
                    }
                } else {
                    outputText.text = "❌ Wrong! Output: $result\nExpected: ${problem.answer}"
                }
            }
        }
    }

    private fun disableInputs() {
        runButton.isEnabled = false
        codeInput.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        jsExecutor.close()
    }
}