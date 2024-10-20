package com.serenitysystems.livable.ui.wochenplan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.R
import com.serenitysystems.livable.ui.wochenplan.data.DynamicTask
import java.text.SimpleDateFormat
import java.util.*

class WochenplanViewModel : ViewModel() {

    private val _tasks = MutableLiveData<List<DynamicTask>>()
    val tasks: LiveData<List<DynamicTask>> = _tasks

    var daysOfWeek: List<String> = listOf() // List of all days displayed by ViewPager

    init {
        loadTasks()
    }

    // Loads a list of tasks for different days; this would typically come from a database
    private fun loadTasks() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Create a few tasks for today, yesterday, and tomorrow
        val tasksList = mutableListOf<DynamicTask>()

        // Task for today
        tasksList.add(
            DynamicTask(
                id = UUID.randomUUID().toString(),
                date = dateFormat.format(calendar.time),
                description = "Küche putzen",
                priority = "Niedrig",
                points = 2,
                assignee = "Yanik",
                avatar = R.drawable.logo,
                additionalDetails = mapOf("duration" to "30 minutes", "tools" to "Mop, Bucket")
            )
        )
        tasksList.add(
            DynamicTask(
                id = UUID.randomUUID().toString(),
                date = dateFormat.format(calendar.time),
                description = "Einkaufen gehen: Deko für die Wg kaufen.",
                priority = "Niedrig",
                points = 2,
                assignee = "Haneen",
                avatar = R.drawable.pp,
                additionalDetails = mapOf("duration" to "30 minutes", "tools" to "Mop, Bucket")
            )
        )

        // Task for tomorrow
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        tasksList.add(
            DynamicTask(
                id = UUID.randomUUID().toString(),
                date = dateFormat.format(calendar.time),
                description = "Wohnzimmer wischen",
                priority = "Hoch",
                points = 4,
                assignee = "Lorenz",
                avatar = R.drawable.logo,
                additionalDetails = mapOf("duration" to "45 minutes", "tools" to "Vacuum cleaner")
            )
        )

        // Task for yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -2)
        tasksList.add(
            DynamicTask(
                id = UUID.randomUUID().toString(),
                date = dateFormat.format(calendar.time),
                description = "Bad reinigen",
                priority = "Mittel",
                points = 3,
                assignee = "Haneen",
                avatar = R.drawable.logo,
                additionalDetails = mapOf("duration" to "40 minutes", "tools" to "Scrub, Cleaner")
            )
        )

        // Reset the calendar to today
        calendar.add(Calendar.DAY_OF_YEAR, 1)

        _tasks.value = tasksList
    }

    // Adds a new task to the current task list
    fun addTask(task: DynamicTask) {
        val updatedTasks = _tasks.value.orEmpty() + task
        _tasks.value = updatedTasks
    }

    // Updates an existing task by replacing it in the list
    fun updateTask(updatedTask: DynamicTask) {
        val updatedTasks = _tasks.value?.map { if (it.id == updatedTask.id) updatedTask else it }
        _tasks.value = updatedTasks
    }

    // Deletes a task by removing it from the list
    fun deleteTask(task: DynamicTask) {
        val updatedTasks = _tasks.value?.filter { it.id != task.id }
        _tasks.value = updatedTasks
    }
}
