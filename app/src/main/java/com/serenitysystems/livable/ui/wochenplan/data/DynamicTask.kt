package com.serenitysystems.livable.ui.wochenplan.data

import com.serenitysystems.livable.R

data class DynamicTask(
    val id: String,
    val date: String, // Date format for tasks (e.g., "20 October 2024")
    val description: String,
    val priority: String,
    val points: Int,
    val assignee: String,
    val avatar: Int = R.drawable.logo,
    val isDone: Boolean = false, // Task completion status

    val isRepeating: Boolean = false, // Indicates if the task repeats
    val repeatFrequency: String? = null, // Repeat frequency (e.g., "daily", "weekly", "monthly")
    val repeatDay: String? = null, // Specific day for repeating tasks

    val additionalDetails: Map<String, String> = emptyMap() // Dynamic details for the task
)
