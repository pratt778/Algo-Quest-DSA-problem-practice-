package com.example.algoquest.Auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object AuthManager {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = task.result?.user?.uid
                    if (userId != null) {
                        // Create user document in Firestore
                        val userData = hashMapOf(
                            "points" to 0L,
                            "solvedProblems" to emptyList<String>()
                        )
                        FirebaseFirestore.getInstance().collection("users")
                            .document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                callback(true, null) // Only call success after Firestore write
                            }
                            .addOnFailureListener { e ->
                                // Delete the Firebase Auth user if Firestore write fails
                                FirebaseAuth.getInstance().currentUser?.delete()
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