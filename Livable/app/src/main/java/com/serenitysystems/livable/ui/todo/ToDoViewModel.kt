package com.serenitysystems.livable.ui.todo

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class ToDoViewModel : ViewModel() {
    var taskItems = MutableLiveData<MutableList<ToDoItem>>()

    init {
        taskItems.value = mutableListOf()
    }

    fun addTaskItem(newTask: ToDoItem){
        val list = taskItems.value ?: mutableListOf()
        list.add(newTask)
        taskItems.postValue(list)
    }

    fun updateTaskItem(id: UUID, name: String, desc: String, dueTime: LocalTime?){
        val list = taskItems.value ?: return
        val task = list.find { it.id == id } ?: return
        task.name = name
        task.desc = desc
        task.dueTime = dueTime
        taskItems.postValue(list)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun setCompleted(taskItem: ToDoItem){
        val list = taskItems.value ?: return
        val task = list.find { it.id == taskItem.id } ?: return
        if (task.completedDate == null)
            task.completedDate = LocalDate.now()
        taskItems.postValue(list)
    }
}
