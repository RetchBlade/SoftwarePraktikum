package com.serenitysystems.livable.ui.todo.data

import java.util.Date
import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val date: Date,
    val isDone: Boolean = false
)

