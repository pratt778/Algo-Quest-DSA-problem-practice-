package com.example.algoquest.utils



import com.example.algoquest.model.Problem
import kotlin.random.Random

object SmartShuffle {

    /**
     * Shuffles problems for main page:
     * - Adapts to user points
     * - Avoids repeats
     * - Respects skill level
     */
    fun shuffle(
        problems: List<Problem>,
        userPoints: Long,
        recentlyShown: Set<String> = emptySet(),
        maxResults: Int = 30
    ): List<Problem> {
        // Step 1: Filter out recently shown
        val available = problems.filter { it.id !in recentlyShown }
        if (available.isEmpty()) return emptyList()

        // Step 2: Bucket by difficulty proximity
        val buckets = bucketBySkill(available, userPoints)
        buckets.forEach { it.shuffle() } // Fisher-Yates inside each

        // Step 3: Weighted random selection
        return weightedPick(buckets, maxResults)
    }

    // Bucket: too easy | easy | just right | hard | too hard
    private fun bucketBySkill(problems: List<Problem>, userPoints: Long): List<MutableList<Problem>> {
        val buckets = Array(5) { mutableListOf<Problem>() }

        problems.forEach { p ->
            val diff = p.points - userPoints
            val index = when {
                diff <= -150 -> 0
                diff <= -50 -> 1
                diff <= 50 -> 2
                diff <= 150 -> 3
                else -> 4
            }
            buckets[index].add(p)
        }
        return buckets.toList()
    }

    // Pick with bias: center bucket 5x more likely
    private fun weightedPick(buckets: List<List<Problem>>, count: Int): List<Problem> {
        val weights = intArrayOf(1, 3, 6, 3, 1) // total = 14
        val result = mutableListOf<Problem>()
        val copy = buckets.map { it.toMutableList() }

        repeat(count) {
            if (copy.all { it.isEmpty() }) return@repeat

            var roll = Random.nextInt(weights.sum())
            var bucketIdx = 0
            for (i in weights.indices) {
                if (roll < weights[i]) {
                    bucketIdx = i
                    break
                }
                roll -= weights[i]
            }

            val bucket = copy[bucketIdx]
            if (bucket.isNotEmpty()) {
                result.add(bucket.removeAt(bucket.size - 1))
            }
        }
        return result
    }
}