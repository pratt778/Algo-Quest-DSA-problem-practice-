package com.example.algoquest.Auth

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

object AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var oneTapClient: SignInClient? = null
    private var signInRequest: BeginSignInRequest? = null

    // Initialize Google Sign-In
    fun initializeGoogleSignIn(context: Context, webClientId: String) {
        oneTapClient = Identity.getSignInClient(context)
        signInRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .setAutoSelectEnabled(false)
            .build()
    }

    // Start Google Sign-In flow
    fun startGoogleSignIn(
        context: Context,
        launcher: ActivityResultLauncher<IntentSenderRequest>,
        onError: (String) -> Unit
    ) {
        oneTapClient?.beginSignIn(signInRequest!!)
            ?.addOnSuccessListener { result ->
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                    launcher.launch(intentSenderRequest)
                } catch (e: Exception) {
                    onError("Failed to launch sign-in: ${e.message}")
                }
            }
            ?.addOnFailureListener { e ->
                onError("Google Sign-In failed: ${e.message}")
            }
    }

    // Handle Google Sign-In result
    fun handleGoogleSignInResult(
        data: android.content.Intent?,
        onComplete: (Boolean, String?) -> Unit
    ) {
        try {
            val credential = oneTapClient?.getSignInCredentialFromIntent(data)
            val idToken = credential?.googleIdToken

            if (idToken != null) {
                val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = task.result?.user?.uid
                            if (userId != null) {
                                // Check if user document exists, create if it doesn't
                                firestore.collection("users").document(userId)
                                    .get()
                                    .addOnSuccessListener { document ->
                                        if (!document.exists()) {
                                            // Create new user document for Google sign-in
                                            val userData = hashMapOf(
                                                "points" to 0L,
                                                "solvedProblems" to emptyList<String>()
                                            )
                                            firestore.collection("users")
                                                .document(userId)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    onComplete(true, null)
                                                }
                                                .addOnFailureListener { e ->
                                                    // Delete the Firebase Auth user if Firestore write fails
                                                    auth.currentUser?.delete()
                                                    onComplete(false, "Failed to create user data: ${e.message}")
                                                }
                                        } else {
                                            // User document already exists (returning user)
                                            onComplete(true, null)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        onComplete(false, "Failed to check user data: ${e.message}")
                                    }
                            } else {
                                onComplete(false, "User ID is null")
                            }
                        } else {
                            onComplete(false, task.exception?.message)
                        }
                    }
            } else {
                onComplete(false, "No ID token received")
            }
        } catch (e: Exception) {
            onComplete(false, e.message)
        }
    }

    fun currentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signIn(email: String, password: String, onComplete: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, null)
                } else {
                    onComplete(false, task.exception?.message)
                }
            }
    }

    fun signUp(email: String, password: String, callback: (Boolean, String?) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid
                    if (userId != null) {
                        // Create user document in Firestore
                        val userData = hashMapOf(
                            "points" to 0L,
                            "solvedProblems" to emptyList<String>()
                        )
                        firestore.collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                callback(true, null)
                            }
                            .addOnFailureListener { e ->
                                // Delete the Firebase Auth user if Firestore write fails
                                auth.currentUser?.delete()
                                callback(false, "Failed to create user data: ${e.message}")
                            }
                    } else {
                        callback(false, "User ID is null")
                    }
                } else {
                    callback(false, task.exception?.message)
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }
}