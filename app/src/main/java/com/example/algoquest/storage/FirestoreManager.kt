package com.example.algoquest.storage

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

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

    fun markProblemSolved(userId: String, problemId: String) {
        db.collection("users").document(userId)
            .update("solvedProblems", FieldValue.arrayUnion(problemId))
    }

    fun getProblems(onResult: (List<Map<String, Any>>) -> Unit) {
        db.collection("problems").get()
            .addOnSuccessListener { query ->
                val problems = query.documents.mapNotNull { it.data }
                onResult(problems)
            }
    }

    fun incrementPoints(userId: String, pointsToAdd: Int) {
        val userRef = db.collection("users").document(userId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentPoints = snapshot.getLong("points") ?: 0L
            transaction.update(userRef, "points", currentPoints + pointsToAdd)
        }
    }

}
