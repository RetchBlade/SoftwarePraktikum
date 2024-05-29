package com.example.todolist3

import com.serenitysystems.livable.ui.todo.ToDoItem

interface ToDoClickListener
{
    fun editTaskItem(taskItem: ToDoItem)
    fun completeTaskItem(taskItem: ToDoItem)
}