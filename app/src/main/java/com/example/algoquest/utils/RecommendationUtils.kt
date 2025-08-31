package com.example.algoquest.utils

import com.example.algoquest.model.Problem

object RecommendationUtils {

    fun recommendUsingDijkstra(current: Problem, allProblems: List<Problem>, topN: Int = 3): List<Problem> {
        val graph = buildGraph(allProblems)
        val distances = dijkstra(current.id, graph)
        return distances
            .filter { it.key != current.id }
            .toList()
            .sortedBy { it.second }
            .take(topN)
            .map { id -> allProblems.first { it.id == id.first } }
    }

    private fun buildGraph(problems: List<Problem>): Map<String, List<Pair<String, Int>>> {
        val graph = mutableMapOf<String, MutableList<Pair<String, Int>>>()

        for (i in problems.indices) {
            for (j in problems.indices) {
                if (i != j) {
                    val p1 = problems[i]
                    val p2 = problems[j]
                    val weight = calculateWeight(p1, p2)
                    graph.getOrPut(p1.id) { mutableListOf() }.add(p2.id to weight)
                }
            }
        }
        return graph
    }

    private fun calculateWeight(p1: Problem, p2: Problem): Int {
        var weight = 10 // base cost
        if (p1.category == p2.category) weight -= 5
        weight -= p1.tags.intersect(p2.tags).size
        weight += kotlin.math.abs(p1.points - p2.points) / 10
        return if (weight < 1) 1 else weight
    }

    private fun dijkstra(start: String, graph: Map<String, List<Pair<String, Int>>>): Map<String, Int> {
        val distances = mutableMapOf<String, Int>().withDefault { Int.MAX_VALUE }
        distances[start] = 0
        val visited = mutableSetOf<String>()

        while (visited.size < graph.size) {
            val u = distances.filter { !visited.contains(it.key) }.minByOrNull { it.value }?.key ?: break
            visited.add(u)

            for ((v, w) in graph[u] ?: emptyList()) {
                val newDist = distances.getValue(u) + w
                if (newDist < distances.getValue(v)) {
                    distances[v] = newDist
                }
            }
        }
        return distances
    }
}
