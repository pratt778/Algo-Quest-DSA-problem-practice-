package com.example.algoquest

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.algoquest.adapter.ProblemAdapter
import com.example.algoquest.utils.UserProgress
import com.example.algoquest.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.problemsRecyclerView)
        val progressText = findViewById<TextView>(R.id.progressText)
        progressText.text = "Your Points: ${UserProgress.currentPoints}"

        val adapter = ProblemAdapter { problem ->
            // Open detail activity
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("problem", problem)
            startActivity(intent)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        viewModel.problems.observe(this) { problems ->
            adapter.updateData(problems)
        }

        viewModel.loadProblems()
    }
}
