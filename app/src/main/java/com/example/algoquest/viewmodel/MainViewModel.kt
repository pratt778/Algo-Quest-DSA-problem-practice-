package com.example.algoquest.viewmodel



import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.algoquest.js.JsExecutor
import com.example.algoquest.model.Problem
import com.example.algoquest.repository.ProblemRepository
import com.example.algoquest.storage.FirestoreManager

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

    private val _points = MutableLiveData<Long>(0L)
    val points: LiveData<Long> = _points

    fun observeUserPoints(userId: String) {
        FirestoreManager.observeUserData(userId).observeForever { data ->
            val pts = data?.get("points") as? Long ?: 0L
            _points.postValue(pts)
        }
    }

    override fun onCleared() {
        jsExecutor.close()
        super.onCleared()
    }
}
