package com.serenitysystems.livable.ui.todo

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.ui.todo.data.ToDoItem

class ToDoViewModel : ViewModel() {
    private val _tasks: MutableLiveData<List<ToDoItem>> = MutableLiveData()
    val tasks: LiveData<List<ToDoItem>> = _tasks

    init {
        _tasks.value = emptyList()
    }

    fun addTask(task: ToDoItem) {
        val currentTasks = _tasks.value?.toMutableList() ?: mutableListOf()
        currentTasks.add(task)
        _tasks.value = currentTasks.toList()
    }
}