package com.serenitysystems.livable.ui.wochenplan.data

import com.serenitysystems.livable.R


data class DynamicTask(
    val id: String = "", // Standardwert für id
    val date: String = "", // Standardwert für date
    val description: String = "", // Standardwert für description
    var priority: String = "", // Standardwert für priority
    val points: Int = 0, // Standardwert für points
    var assignee: String = "Unassigned", // Updated: Default to "Unassigned"
    var assigneeEmail: String = "", // Updated: Make mutable for claimable logic
    val avatar: Int = R.drawable.logo, // Standardwert für avatar
    var isDone: Boolean = false, // Standardwert für isDone
    val isRepeating: Boolean = false, // Standardwert für isRepeating
    val repeatFrequency: String? = null, // Optional, also null erlaubt
    val repeatDay: String? = null, // Optional, also null erlaubt
    val additionalDetails: Map<String, String> = emptyMap(), // Standardwert für additionalDetails
    var isClaimable: Boolean = false // New: Indicates if the task is claimable
)
