package com.example.algoquest.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object UserProgress {

    private val solvedProblems = mutableSetOf<String>()
    var currentPoints: Int = 0
        private set

    private val db = FirebaseFirestore.getInstance()

    fun addPoints(points: Int) {
        currentPoints += points
    }

    fun markProblemSolvedLocally(problemId: String) {
        solvedProblems.add(normalizeId(problemId))
    }

    fun hasSolved(problemId: String): Boolean {
        return solvedProblems.contains(normalizeId(problemId))
    }

    fun getSolvedProblems(): Set<String> {
        return solvedProblems
    }

    private fun normalizeId(problemId: String): String {
        return problemId.trim().lowercase()
    }

    /**
     * Sync solved problems from Firestore into local memory
     */
    fun syncFromFirestore(userId: String, onComplete: (() -> Unit)? = null) {
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val solvedList = document.get("solvedProblems") as? List<String> ?: emptyList()
                solvedProblems.clear()
                solvedList.forEach { solvedProblems.add(normalizeId(it)) }
                Log.d("FirestoreSync", "Loaded solved problems: $solvedProblems")
                onComplete?.invoke()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "Error loading solved problems", e)
                onComplete?.invoke()
            }
    }

    /**
     * Add a solved problem to Firestore and local memory
     */
    fun markProblemSolved(userId: String, problemId: String) {
        val normalizedId = normalizeId(problemId)
        if (!solvedProblems.contains(normalizedId)) {
            solvedProblems.add(normalizedId)
            db.collection("users")
                .document(userId)
                .update("solvedProblems", FieldValue.arrayUnion(normalizedId))
                .addOnSuccessListener {
                    Log.d("FirestoreUpdate", "Added $normalizedId to solvedProblems")
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreUpdate", "Failed to update Firestore", e)
                }
        }
    }
}
