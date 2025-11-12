package com.example.algoquest.storage

import android.R
import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlin.text.get
import kotlin.text.set

object FirestoreManager {

    private val db = FirebaseFirestore.getInstance()

    fun getUserData(userId: String, onResult: (Map<String, Any>?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                onResult(doc.data)
            }
            .addOnFailureListener { onResult(null) }
    }

    fun updatePoints(userId: String, points: Int) {
        db.collection("users").document(userId)
            .update("points", points)
    }

    fun markProblemSolved(userId: String, problemId: String, callback: (Boolean) -> Unit) {
        db.collection("users").document(userId)
            .update("solvedProblems", FieldValue.arrayUnion(problemId))
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }


    fun getProblems(onResult: (List<Map<String, Any>>) -> Unit) {
        db.collection("problems").get()
            .addOnSuccessListener { query ->
                val problems = query.documents.mapNotNull { it.data }
                onResult(problems)
            }
    }

    fun incrementPoints(userId: String, pointsToAdd: Int, callback: (Boolean) -> Unit) {
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentPoints = snapshot.getLong("points") ?: 0L
            transaction.update(userRef, "points", currentPoints + pointsToAdd)
        }.addOnSuccessListener {
            callback(true) // transaction succeeded
        }.addOnFailureListener { e ->
            Log.e("FirestoreManager", "Failed to increment points", e)
            callback(false) // transaction failed
        }
    }

    // In FirestoreManager.kt
    fun observeUserData(userId: String): LiveData<Map<String, Any>?> {
        val liveData = MutableLiveData<Map<String, Any>?>()
        val docRef = Firebase.firestore.collection("users").document(userId)

        val listener = docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w("FirestoreManager", "Listen failed.", e)
                liveData.postValue(null)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                liveData.postValue(snapshot.data)
            } else {
                liveData.postValue(null)
            }
        }

        // Optional: Remove listener when not needed (not critical for simple cases)
        // You can manage lifecycle if needed, but for basic use, it's okay.

        return liveData
    }
    fun saveProblemSolution(userId: String, problemId: String, solution: String, callback: (Boolean) -> Unit) {
        val solutionsRef = Firebase.firestore.collection("users").document(userId)
            .collection("solutions").document(problemId)

        solutionsRef.set(mapOf(
            "code" to solution,
            "timestamp" to System.currentTimeMillis()
        ))
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun getProblemSolution(userId: String, problemId: String, callback: (String?) -> Unit) {
        val solutionsRef = Firebase.firestore.collection("users").document(userId)
            .collection("solutions").document(problemId)

        solutionsRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    callback(document.getString("code"))
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
