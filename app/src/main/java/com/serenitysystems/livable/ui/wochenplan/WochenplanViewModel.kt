package com.serenitysystems.livable.ui.wochenplan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.wochenplan.data.Task

class WochenplanViewModel : ViewModel() {

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> = _tasks

    init {
        loadTasks()
    }

    private fun loadTasks() {
        // Hier könntest du die Aufgaben aus einer Datenquelle laden. Für das Beispiel sind sie hartcodiert.
        val tasksList = listOf(
            Task("Sonntag", "Küche putzen", "Niedrig", 2, "Haneen", R.drawable.logo),
            Task("Sonntag", "Wohnzimmer wischen", "Hoch", 2, "Lorenz", R.drawable.logo),
            Task("Montag", "Einkaufen gehen", "Mittel", 1, "Yanik", R.drawable.logo),
            Task("Freitag", "Einkaufen gehen", "Mittel", 1, "Yanik", R.drawable.logo),
        )
        _tasks.value = tasksList
    }
    fun addTask(task: Task) {
        val updatedTasks = _tasks.value.orEmpty() + task
        _tasks.value = updatedTasks
    }
}
