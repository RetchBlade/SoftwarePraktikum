package com.serenitysystems.livable.ui.wochenplan.data

import com.serenitysystems.livable.R


data class DynamicTask(
    val id: String = "",
    val date: String = "",
    val description: String = "",
    var priority: String = "",
    var points: Int = 0,
    var assignee: String = "Unassigned",
    var assigneeEmail: String = "",
    var isDone: Boolean = false,
    val repeating: Boolean = false,
    val repeatFrequency: String? = null,
    val repeatDay: String? = null,
    val additionalDetails: Map<String, String> = emptyMap(),
    var wasUpdated: Boolean = false, // Speichert, ob die Punkte bereits angepasst wurden
    var isAccounted: Boolean = false, // Speichert, ob die Aufgabe bereits in die Punkteberechnung einbezogen wurde.
    val repeatUntil: String? = null,
    val parentTaskId: String? = null


)

