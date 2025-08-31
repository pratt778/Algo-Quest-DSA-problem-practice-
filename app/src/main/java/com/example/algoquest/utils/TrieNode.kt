package com.example.algoquest.utils

// TrieNode.kt
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    var isEndOfWord = false
}

// Trie.kt
class Trie {
    private val root = TrieNode()

    fun insert(word: String) {
        var node = root
        for (char in word) {
            node = node.children.getOrPut(char) { TrieNode() }
        }
        node.isEndOfWord = true
    }

    fun searchPrefix(prefix: String): List<String> {
        var node = root
        for (char in prefix) {
            node = node.children[char] ?: return emptyList()
        }
        val results = mutableListOf<String>()
        collectWords(node, prefix, results)
        return results
    }

    private fun collectWords(node: TrieNode, prefix: String, results: MutableList<String>) {
        if (node.isEndOfWord) results.add(prefix)
        for ((char, child) in node.children) {
            collectWords(child, prefix + char, results)
        }
    }
}
