package com.example.algoquest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.algoquest.Auth.AuthManager

class SignUpActivity : ComponentActivity() {

    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var signUpButton: Button
    private lateinit var loginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize UI elements
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput)
        signUpButton = findViewById(R.id.signUpButton)
        loginLink = findViewById(R.id.loginLink)

        // Sign-up button click listener
        signUpButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            AuthManager.signUp(email, password) { success, errorMessage ->
                if (success) {
                    Toast.makeText(this, "Sign-up successful", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                } else {
                    Toast.makeText(this, "Sign-up failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Login link click listener
        loginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        // Pass any problem data if needed, e.g., intent.putExtra("problem", problem)
        startActivity(intent)
        finish()
    }
}