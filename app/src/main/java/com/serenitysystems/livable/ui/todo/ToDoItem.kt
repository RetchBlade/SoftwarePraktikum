package com.serenitysystems.livable.ui.todo

import android.content.Context
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.LocalTime
import com.serenitysystems.livable.R
import java.util.UUID

class ToDoItem(
    var name: String, var desc: String, var dueTime: LocalTime?,var completedDate: LocalDate?, var id: UUID = UUID.randomUUID()){
        fun isCompleted() = completedDate != null
        fun imageResource(): Int = if(isCompleted()) R.drawable.checked_24 else R.drawable.unchecked_24
        fun imageColor(context: Context): Int = if(isCompleted()) purple(context) else black(context)

        private fun purple(context: Context) = ContextCompat.getColor(context, R.color.black)
        private fun black(context: Context) = ContextCompat.getColor(context, R.color.black)
}

