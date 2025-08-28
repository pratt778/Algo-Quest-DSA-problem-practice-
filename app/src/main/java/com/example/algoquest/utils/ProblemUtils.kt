package com.example.algoquest.utils


import com.example.algoquest.model.Problem
import java.util.ArrayDeque

object ProblemUtils {

    fun sortProblemsTopologically(problems: List<Problem>): List<Problem> {
        val inDegree = mutableMapOf<String, Int>()
        val graph = mutableMapOf<String, MutableList<String>>()

        // Build graph and in-degree map
        problems.forEach { problem ->
            inDegree[problem.id] = problem.prerequisites.size
            problem.prerequisites.forEach { prereq ->
                graph.getOrPut(prereq) { mutableListOf() }.add(problem.id)
            }
        }

        val queue = ArrayDeque<String>()
        val result = mutableListOf<Problem>()

        // Start with problems having no prerequisites
        inDegree.filter { it.value == 0 }.forEach { queue.add(it.key) }

        while (queue.isNotEmpty()) {
            val currentId = queue.removeFirst()
            val problem = problems.find { it.id == currentId } ?: continue
            result.add(problem)

            graph[currentId]?.forEach { dependentId ->
                inDegree[dependentId] = inDegree[dependentId]!! - 1
                if (inDegree[dependentId] == 0) {
                    queue.add(dependentId)
                }
            }
        }

        return result
    }

    fun getUnlockedProblems(problems: List<Problem>): List<Problem> {
        return problems.filter { problem ->
            problem.prerequisites.all { prereqId ->
                UserProgress.hasSolved(prereqId)
            }
        }
    }
}
