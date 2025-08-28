package com.example.algoquest.js

import app.cash.quickjs.QuickJs

class JsExecutor {

    private var quickJs: QuickJs? = null

    init {
        quickJs = QuickJs.create()
        setupConsole()
    }

    private fun setupConsole() {
        quickJs?.evaluate("""
            var console = {
                log: function() {
                    var args = Array.prototype.slice.call(arguments);
                    var message = args.map(function(arg) {
                        return typeof arg === 'object' ? JSON.stringify(arg) : String(arg);
                    }).join(' ');
                    if (typeof globalThis.__consoleLogs === 'undefined') globalThis.__consoleLogs = [];
                    globalThis.__consoleLogs.push(message);
                },
                error: function() { console.log.apply(console, arguments); },
                warn: function() { console.log.apply(console, arguments); }
            };
        """.trimIndent())
    }

    fun execute(code: String): String {
        try {
            quickJs?.evaluate("if (typeof globalThis.__consoleLogs !== 'undefined') globalThis.__consoleLogs = [];")
            val result = quickJs?.evaluate(code)?.toString()  // Cast to String

            val consoleOutput = quickJs?.evaluate("""
            if (typeof globalThis.__consoleLogs !== 'undefined' && globalThis.__consoleLogs.length > 0) {
                globalThis.__consoleLogs.join('\n');
            } else {
                '';
            }
        """.trimIndent())?.toString()  // Cast to String

            val finalOutput = StringBuilder()
            if (!consoleOutput.isNullOrEmpty()) finalOutput.append(consoleOutput)
            if (!result.isNullOrEmpty()) {
                if (finalOutput.isNotEmpty()) finalOutput.append("\n\n")
                finalOutput.append("Return: ").append(result)
            }
            return if (finalOutput.isNotEmpty()) finalOutput.toString() else "No output"
        } catch (e: Exception) {
            return "Error: ${e.message}"
        }
    }


    fun close() {
        quickJs?.close()
    }
}
