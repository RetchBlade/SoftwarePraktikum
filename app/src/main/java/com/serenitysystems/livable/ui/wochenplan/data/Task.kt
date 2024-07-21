package com.serenitysystems.livable.ui.wochenplan.data

data class Task(
    val day: String,
    val description: String,
    val priority: String,
    val points: Int,
    val assignee: String,
    val avatar: Int
)