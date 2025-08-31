package com.example.algoquest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.material3.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.algoquest.Auth.AuthManager
import com.example.algoquest.adapter.ProblemAdapter
import com.example.algoquest.storage.FirestoreManager
import com.example.algoquest.utils.ProblemUtils
import com.example.algoquest.utils.UserProgress
import com.example.algoquest.viewmodel.MainViewModel
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var logoutButtons: Button

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        val currentUser = AuthManager.currentUser()
        if (currentUser == null) {
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
            return
        }
        val userId = currentUser.uid

        // UI Elements
        val recyclerView: RecyclerView = findViewById(R.id.problemsRecyclerView)
        val progressText = findViewById<TextView>(R.id.progressText)
        val userName = findViewById<TextView>(R.id.user)
         logoutButtons = findViewById<Button>(R.id.logoutButton)

        // Adapter
        val adapter = ProblemAdapter { problem ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("problem", problem)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Fetch points
        FirestoreManager.getUserData(userId) { data ->
            val points = data?.get("points") as? Long ?: 0L
            progressText.text = "Your Points: $points"
            userName.text="${currentUser.email}"
        }

        // ✅ Sync solved problems first
        UserProgress.syncFromFirestore(userId) {
            Log.d("MainActivity", "User progress synced")

            // Now observe problems after sync
            viewModel.problems.observe(this) { problems ->
                try {
                    val sortedProblems = ProblemUtils.sortProblemsTopologically(problems)
                    val unlockedProblems = ProblemUtils.getUnlockedProblems(sortedProblems)
                    adapter.updateData(unlockedProblems)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error processing problems", e)
                    adapter.updateData(emptyList())
                }
            }

            // Load problems AFTER sync
            viewModel.loadProblems()
        }

        logoutButtons.setOnClickListener {
            AuthManager.signOut()
            startActivity(Intent(this, SignUpActivity::class.java))
            finish()
        }

    }

}