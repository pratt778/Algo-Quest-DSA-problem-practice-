package com.example.algoquest.viewmodel



import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.algoquest.js.JsExecutor
import com.example.algoquest.model.Problem
import com.example.algoquest.repository.ProblemRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val jsExecutor = JsExecutor()
    private val repository = ProblemRepository(application)

    private val _output = MutableLiveData<String>()
    val output: LiveData<String> get() = _output

    private val _problems = MutableLiveData<List<Problem>>()
    val problems: LiveData<List<Problem>> get() = _problems

    fun runCode(code: String) {
        _output.value = jsExecutor.execute(code)
    }

    fun loadProblems(jsonFile: String = "problems.json") {
        _problems.value = repository.loadProblemsFromAssets(jsonFile)
    }

    override fun onCleared() {
        jsExecutor.close()
        super.onCleared()
    }
}
