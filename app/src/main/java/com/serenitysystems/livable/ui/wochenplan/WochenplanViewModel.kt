package com.serenitysystems.livable.ui.wochenplan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.wochenplan.data.Task
import java.util.UUID

class WochenplanViewModel : ViewModel() {
    // Diese LiveData speichert die Aufgabenliste
    private val _tasks = MutableLiveData<List<Task>>()
    // Diese LiveData wird vom View verwendet
    val tasks: LiveData<List<Task>> = _tasks

    // Diese Funktion wird beim Erstellen des ViewModels ausgeführt
    init {
        loadTasks()
    }

    // Diese Funktion wird beim Laden des ViewModels ausgeführt
    private fun loadTasks() {
        // Hier könntest du die Aufgaben aus einer Datenquelle laden. Für das Beispiel sind sie hartcodiert.
        val tasksList = listOf(
            Task(id = UUID.randomUUID().toString(), day = "Sonntag", description = "Küche putzen", priority = "Niedrig", points = 2, assignee = "Haneen", avatar = R.drawable.logo),
            Task(id = UUID.randomUUID().toString(), day = "Sonntag", description = "Wohnzimmer wischen", priority = "Hoch", points = 2, assignee = "Lorenz", avatar = R.drawable.logo),
            Task(id = UUID.randomUUID().toString(), day = "Montag", description = "Einkaufen gehen", priority = "Mittel", points = 1, assignee = "Yanik", avatar = R.drawable.logo),
            Task(id = UUID.randomUUID().toString(), day = "Freitag", description = "Einkaufen gehen", priority = "Mittel", points = 1, assignee = "Yanik", avatar = R.drawable.logo)
        )
        _tasks.value = tasksList
    }

    // Diese Funktion fügt eine neue Aufgabe hinzu
    fun addTask(task: Task) {
        val updatedTasks = _tasks.value.orEmpty() + task // Hier wird die neue Aufgabe hinzugefügt
        _tasks.value = updatedTasks // Hier wird die neue Aufgabenliste aktualisiert
    }

    // Diese Funktion aktualisiert eine Aufgabe
    fun updateTask(updatedTask: Task) {
        val updatedTasks = _tasks.value?.map { if (it.id == updatedTask.id) updatedTask else it }
        _tasks.value = updatedTasks
    }

    // Diese Funktion löscht eine Aufgabe
    fun deleteTask(task: Task) {
        val updatedTasks = _tasks.value?.filter { it.id != task.id }
        _tasks.value = updatedTasks
    }
}
