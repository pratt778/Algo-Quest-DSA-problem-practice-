package com.example.algoquest.repository

import android.content.Context
import com.example.algoquest.model.Problem
import com.example.algoquest.utils.FileUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ProblemRepository(private val context: Context) {

    fun loadProblemsFromAssets(fileName: String): List<Problem> {
        val jsonString = FileUtils.readJsonFromAssets(context, fileName)
        val type = object : TypeToken<List<Problem>>() {}.type
        return Gson().fromJson(jsonString, type)
    }
}
