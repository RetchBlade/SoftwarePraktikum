package com.serenitysystems.livable.ui.wochenplan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.wochenplan.data.Task

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
            Task("Sonntag", "Küche putzen", "Niedrig", 2, "Haneen", R.drawable.logo),
            Task("Sonntag", "Wohnzimmer wischen", "Hoch", 2, "Lorenz", R.drawable.logo),
            Task("Montag", "Einkaufen gehen", "Mittel", 1, "Yanik", R.drawable.logo),
            Task("Freitag", "Einkaufen gehen", "Mittel", 1, "Yanik", R.drawable.logo),
        )
        // Hier wird die Aufgabenliste aktualisiert
        _tasks.value = tasksList
    }
    // Diese Funktion fügt eine neue Aufgabe hinzu
    fun addTask(task: Task) {
        val updatedTasks = _tasks.value.orEmpty() + task // Hier wird die neue Aufgabe hinzugefügt
        _tasks.value = updatedTasks // Hier wird die neue Aufgabenliste aktualisiert
    }
}
