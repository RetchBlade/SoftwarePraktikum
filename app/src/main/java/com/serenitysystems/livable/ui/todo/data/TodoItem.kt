package com.serenitysystems.livable.ui.todo.data

import java.util.Date

data class TodoItem(
    val id: String = "",
    val description: String = "",
    val detailedDescription: String = "",
    val date: Date = Date(),  // Oder String, falls du das so bevorzugst
    val priority: String = "",
    val isDone: Boolean = false,
    val repeatType: String? = null // Optional, je nach Bedarf
)


