package com.example.algoquest.utils

import com.example.algoquest.model.Problem

object FuzzyMatcher {

    private const val TITLE_WEIGHT = 1.0
    private const val DESCRIPTION_WEIGHT = 0.3
    private const val TAG_WEIGHT = 0.7
    private const val CATEGORY_WEIGHT = 0.6
    private const val MATCH_THRESHOLD = 0.4 // min normalized score to count as match

    /**
     * Returns a match score between 0.0 (no match) and 1.0 (perfect match)
     */
    fun scoreMatch(query: String, text: String): Double {
        if (query.isEmpty() || text.isEmpty()) return 0.0

        val cleanQuery = normalize(query)
        val cleanText = normalize(text)

        if (cleanQuery.isEmpty() || cleanText.isEmpty()) return 0.0

        // Exact match gets highest score
        if (cleanQuery == cleanText) return 1.0

        // Check if query is a contiguous substring (fast win)
        if (cleanText.contains(cleanQuery)) return 0.9

        // Use LCS-based similarity for fuzzy match
        val lcsLength = longestCommonSubsequenceLength(cleanQuery, cleanText)
        val maxLength = maxOf(cleanQuery.length, cleanText.length)
        return lcsLength.toDouble() / maxLength
    }

    /**
     * Normalizes text for matching: lowercase + remove non-alphanumeric
     */
    private fun normalize(text: String): String {
        return text.lowercase().filter { it.isLetterOrDigit() || it.isWhitespace() }
    }

    /**
     * Computes length of Longest Common Subsequence (LCS)
     */
    private fun longestCommonSubsequenceLength(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 1..m) {
            for (j in 1..n) {
                if (a[i - 1] == b[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1
                } else {
                    dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }
        return dp[m][n]
    }

    /**
     * Returns true if problem matches query with sufficient relevance
     */
    fun matchesProblem(query: String, problem: Problem): Boolean {
        if (query.isBlank()) return true

        val titleScore = scoreMatch(query, problem.title) * TITLE_WEIGHT
        val descScore = scoreMatch(query, problem.description) * DESCRIPTION_WEIGHT
        val tagScore = problem.tags.maxOfOrNull { scoreMatch(query, it) } ?: 0.0
        val categoryScore = scoreMatch(query, problem.category) * CATEGORY_WEIGHT

        val bestScore = maxOf(titleScore, descScore, tagScore * TAG_WEIGHT, categoryScore)

        return bestScore >= MATCH_THRESHOLD
    }
}