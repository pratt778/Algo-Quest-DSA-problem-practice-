package com.example.algoquest.utils


import android.content.Context
import java.io.BufferedReader

object FileUtils {

    fun readJsonFromAssets(context: Context, fileName: String): String {
        val inputStream = context.assets.open(fileName)
        val bufferedReader = BufferedReader(inputStream.reader())
        return bufferedReader.use { it.readText() }
    }
}
