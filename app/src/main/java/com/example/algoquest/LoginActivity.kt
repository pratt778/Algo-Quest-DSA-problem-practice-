package com.example.algoquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.algoquest.Auth.AuthManager

class LoginActivity : ComponentActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Check if user is already logged in
        if (AuthManager.currentUser() != null) {
            navigateToDetailActivity()
            return
        }

        // Initialize UI elements
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signUpLink = findViewById(R.id.signUpLink)

        // Login button click listener
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AuthManager.signIn(email, password) { success, errorMessage ->
                if (success) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    navigateToDetailActivity()
                } else {
                    Toast.makeText(this, "Login failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Sign-up link click listener
        signUpLink.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun navigateToDetailActivity() {
        val intent = Intent(this, DetailActivity::class.java)
        // Pass any problem data if needed, e.g., intent.putExtra("problem", problem)
        startActivity(intent)
        finish()
    }
}