package com.example.algoquest.utils

object UserProgress {
    var currentPoints: Int = 0
        private set

    private val solvedProblems = mutableSetOf<String>()

    fun addPoints(points: Int) {
        currentPoints += points
    }

    fun markProblemSolved(problemId: String) {
        solvedProblems.add(problemId)
    }

    fun hasSolved(problemId: String): Boolean {
        return solvedProblems.contains(problemId)
    }
}
