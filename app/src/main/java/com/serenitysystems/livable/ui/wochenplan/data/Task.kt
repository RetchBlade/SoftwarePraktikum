package com.serenitysystems.livable.ui.wochenplan.data

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val day: String,
    val description: String,
    val priority: String,
    val points: Int,
    val assignee: String,
    val avatar: Int,
    val isDone: Boolean = false
)

