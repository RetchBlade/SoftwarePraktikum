package com.serenitysystems.livable.ui.todo

interface ToDoClickListener{
    fun editTaskItem(taskItem: ToDoItem)
    fun completeTaskItem(taskItem: ToDoItem)
}