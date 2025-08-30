package com.example.algoquest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.algoquest.Auth.AuthManager
import com.example.algoquest.adapter.ProblemAdapter
import com.example.algoquest.storage.FirestoreManager
import com.example.algoquest.utils.ProblemUtils
import com.example.algoquest.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            FirebaseApp.initializeApp(this)
            Log.d("MainActivity", "Current user: ${AuthManager.currentUser()?.uid}")
            if (AuthManager.currentUser() == null) {
                Log.d("MainActivity", "User is not logged in, redirecting to SignUpActivity")
                startActivity(Intent(this, SignUpActivity::class.java))
                finish()
                return
            }
            // Rest of your code
        } catch (e: Exception) {
            Log.e("MainActivity", "Crash in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Initialize UI elements
        val recyclerView: RecyclerView = findViewById(R.id.problemsRecyclerView)
        val progressText = findViewById<TextView>(R.id.progressText)

        // Fetch user points from Firestore
        val userId = AuthManager.currentUser()?.uid ?: return
        FirestoreManager.getUserData(userId) { data ->
            val points = data?.get("points") as? Long ?: 0L
            progressText.text = "Your Points: $points"
        }

        // Set up RecyclerView and adapter
        val adapter = ProblemAdapter { problem ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("problem", problem)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Observe and display problems
        viewModel.problems.observe(this) { problems ->
            val sortedProblems = ProblemUtils.sortProblemsTopologically(problems)
            val unlockedProblems = ProblemUtils.getUnlockedProblems(sortedProblems)
            adapter.updateData(unlockedProblems) // Only show unlocked problems
        }

        // Load problems
        viewModel.loadProblems()
    }
}