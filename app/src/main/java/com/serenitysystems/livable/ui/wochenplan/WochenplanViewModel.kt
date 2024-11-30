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

    private val _lastWeekTasks = MutableLiveData<List<DynamicTask>>()
    val lastWeekTasks: MutableLiveData<List<DynamicTask>> = _lastWeekTasks

    private val _thisWeekTasks = MutableLiveData<List<DynamicTask>>()
    val thisWeekTasks: MutableLiveData<List<DynamicTask>> = _thisWeekTasks

    private val _nextWeekTasks = MutableLiveData<List<DynamicTask>>()
    val nextWeekTasks: MutableLiveData<List<DynamicTask>> = _nextWeekTasks

    private val _todayTasks = MutableLiveData<List<DynamicTask>>()
    val todayTasks: MutableLiveData<List<DynamicTask>> = _todayTasks

    var daysOfWeek: List<String> = listOf() // List of all days displayed by ViewPager

    init {
        loadTasks()
    }

    // Loads a list of tasks for different days; this would typically come from a database
    private fun loadTasks() {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Create a few tasks for last week, this week, and next week
        val tasksList = mutableListOf<DynamicTask>()

        // Tasks for this week
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Start of the week
        for (i in 0..6) {
            tasksList.add(
                DynamicTask(
                    id = UUID.randomUUID().toString(),
                    date = dateFormat.format(calendar.time),
                    description = "Task for this week - Day ${i + 1}",
                    priority = if (i % 2 == 0) "Mittel" else "Hoch",
                    points = (i + 1) * 2,
                    assignee = "User ${i + 1}",
                    avatar = R.drawable.logo,
                    additionalDetails = mapOf("duration" to "30 minutes", "tools" to "Tools ${i + 1}")
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Tasks for last week
        calendar.add(Calendar.DAY_OF_YEAR, -14) // Go back to the start of last week
        for (i in 0..6) {
            tasksList.add(
                DynamicTask(
                    id = UUID.randomUUID().toString(),
                    date = dateFormat.format(calendar.time),
                    description = "Task for last week - Day ${i + 1}",
                    priority = if (i % 2 == 0) "Niedrig" else "Mittel",
                    points = (i + 1) * 2,
                    assignee = "User Last Week ${i + 1}",
                    avatar = R.drawable.logo,
                    additionalDetails = mapOf("duration" to "20 minutes", "tools" to "Tools Last ${i + 1}")
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Tasks for next week
        calendar.add(Calendar.DAY_OF_YEAR, 7) // Start of next week
        for (i in 0..6) {
            tasksList.add(
                DynamicTask(
                    id = UUID.randomUUID().toString(),
                    date = dateFormat.format(calendar.time),
                    description = "Task for next week - Day ${i + 1}",
                    priority = if (i % 2 == 0) "Hoch" else "Niedrig",
                    points = (i + 1) * 3,
                    assignee = "User Next Week ${i + 1}",
                    avatar = R.drawable.logo,
                    additionalDetails = mapOf("duration" to "40 minutes", "tools" to "Tools Next ${i + 1}")
                )
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

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
