package com.example.algoquest.viewmodel



import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.algoquest.Auth.AuthManager
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {
    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var confirmPassword by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf("")
        private set
    var showToast by mutableStateOf("")
        private set
    var navigateToMain by mutableStateOf(false)
        private set

    fun updateEmail(newEmail: String) {
        email = newEmail.trim()
        clearError()
    }

    fun updatePassword(newPassword: String) {
        password = newPassword.trim()
        clearError()
    }

    fun updateConfirmPassword(newConfirmPassword: String) {
        confirmPassword = newConfirmPassword.trim()
        clearError()
    }

    fun onSignUpClick() {
        viewModelScope.launch {
            // Basic validation
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                showToast = "Please fill out all fields"
                return@launch
            }
            if (password != confirmPassword) {
                showToast = "Passwords do not match"
                return@launch
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast = "Invalid email format"
                return@launch
            }

            // Call AuthManager for sign-up
            AuthManager.signUp(email, password) { success, errorMessage ->
                if (success) {
                    showToast = "Sign-up successful"
                    navigateToMain = true
                } else {
                    showToast = "Sign-up failed: $errorMessage"
                }
            }
        }
    }

    fun onToastShown() {
        showToast = ""
    }

    private fun clearError() {
        errorMessage = ""
    }
}
