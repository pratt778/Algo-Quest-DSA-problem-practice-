package com.example.algoquest

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.algoquest.js.JsExecutor
import com.example.algoquest.model.Problem
import com.example.algoquest.utils.UserProgress

class DetailActivity : ComponentActivity() {

    private lateinit var jsExecutor: JsExecutor

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        val problem = intent.getParcelableExtra<Problem>("problem") ?: return
        val answer: String

        val progressText = findViewById<TextView>(R.id.progressText)

        // Bind problem details
        findViewById<TextView>(R.id.titleText).text = problem.title
        findViewById<TextView>(R.id.descriptionText).text = problem.description
        findViewById<TextView>(R.id.categoryText).text = "Category: ${problem.category}"
        findViewById<TextView>(R.id.pointsText).text = "Points: ${problem.points}"
        findViewById<TextView>(R.id.tagsText).text = "Tags: ${problem.tags.joinToString(", ")}"
        findViewById<TextView>(R.id.hintsText).text = "Hints: ${problem.hints.joinToString(", ")}"

        // JS runtime elements
        val codeInput = findViewById<EditText>(R.id.codeInput)
        val runButton = findViewById<Button>(R.id.runButton)
        val outputText = findViewById<TextView>(R.id.outputText)

        jsExecutor = JsExecutor()

        runButton.setOnClickListener {
            val userCode = codeInput.text.toString().trim()
            if (userCode.isEmpty()) {
                outputText.text = "Please enter some JavaScript code."
            } else {
                val result = jsExecutor.execute(userCode).trim()
                if (result == problem.answer.trim()) {
                    outputText.text = "✅ Correct! Output: $result"
                    runButton.isEnabled = false
                    codeInput.isEnabled = false

                    UserProgress.addPoints(problem.points)
                    progressText.text = "Your Points: ${UserProgress.currentPoints}"
                } else {
                    outputText.text = "❌ Wrong! Output: $result\nExpected: ${problem.answer}"
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        val progressText = findViewById<TextView>(R.id.progressText)
        progressText.text = "Your Points: ${UserProgress.currentPoints}"
    }


    override fun onDestroy() {
        super.onDestroy()
        jsExecutor.close()
    }


}
