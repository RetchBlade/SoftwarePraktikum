package com.serenitysystems.livable.ui.wochenplan

import DayPagerAdapter
import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentWochenplanBinding
import com.serenitysystems.livable.ui.wochenplan.data.DynamicTask
import java.text.SimpleDateFormat
import java.util.*

class WochenplanFragment : Fragment() {
    private var _binding: FragmentWochenplanBinding? = null
    private val binding get() = _binding!!
    private lateinit var wochenplanViewModel: WochenplanViewModel
    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        wochenplanViewModel = ViewModelProvider(this).get(WochenplanViewModel::class.java)

        _binding = FragmentWochenplanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupDaysList()
        setupDayViewPager()

        // Observe tasks and update UI accordingly
        wochenplanViewModel.tasks.observe(viewLifecycleOwner) {
            displayTasksForDay(getSelectedDay())
        }

        // Add new task
        binding.addTaskButton.setOnClickListener {
            showTaskDialog()
        }

        return root
    }
    private fun setupDaysList() {
        val calendar = Calendar.getInstance()
        val days = mutableListOf<String>()

        calendar.add(Calendar.DAY_OF_YEAR, -7)  // Adjust the starting point if needed
        for (i in 0 until 15) {  // Assuming 15 days in your pager
            days.add(dateFormat.format(calendar.time))  // Adds "EEEE, dd MMMM yyyy" format
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        wochenplanViewModel.daysOfWeek = days
    }

    private fun setupDayViewPager() {
        val dayAdapter = DayPagerAdapter(wochenplanViewModel.daysOfWeek) { day ->
            displayTasksForDay(day)
        }
        binding.dayViewPager.adapter = dayAdapter
        binding.dayViewPager.setCurrentItem(7, false)

        // Navigation buttons
        binding.arrowLeft.setOnClickListener {
            if (binding.dayViewPager.currentItem > 0) {
                binding.dayViewPager.currentItem -= 1
            }
        }

        binding.arrowRight.setOnClickListener {
            if (binding.dayViewPager.currentItem < wochenplanViewModel.daysOfWeek.size - 1) {
                binding.dayViewPager.currentItem += 1
            }
        }

        binding.dayViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                displayTasksForDay(wochenplanViewModel.daysOfWeek[position])
            }
        })
    }

    private fun getSelectedDay(): String {
        return wochenplanViewModel.daysOfWeek[binding.dayViewPager.currentItem]
    }

    private fun showTaskDialog(existingTask: DynamicTask? = null) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.wochenplan_dialog_add_task, null)

        val datePickerIcon: ImageView = dialogView.findViewById(R.id.datePickerIcon)
        val dateTextView: TextView = dialogView.findViewById(R.id.dateTextView)
        val taskDescription: EditText = dialogView.findViewById(R.id.taskDescription)
        val taskPriority: Spinner = dialogView.findViewById(R.id.taskPriority)
        val taskPoints: EditText = dialogView.findViewById(R.id.taskPoints)
        val taskAssigneeSpinner: Spinner = dialogView.findViewById(R.id.taskAssigneeSpinner)
        val repeatToggle: Switch = dialogView.findViewById(R.id.repeatingTaskToggle)
        val repeatFrequencyLayout: View = dialogView.findViewById(R.id.repeatingTaskDetails)
        val repeatFrequencySpinner: Spinner = dialogView.findViewById(R.id.repeatFrequencySpinner)
        val repeatDaySpinner: Spinner = dialogView.findViewById(R.id.repeatDaySpinner)

        val selectedDate = Calendar.getInstance()
        existingTask?.let {
            val parsedDate = dateFormat.parse(it.date)
            parsedDate?.let { date -> selectedDate.time = date }
            taskDescription.setText(it.description)
            taskPriority.setSelection(resources.getStringArray(R.array.prio_liste).indexOf(it.priority))
            taskPoints.setText(it.points.toString())
            taskAssigneeSpinner.setSelection(resources.getStringArray(R.array.assignee_list).indexOf(it.assignee))
            repeatToggle.isChecked = it.isRepeating
            repeatFrequencyLayout.visibility = if (it.isRepeating) View.VISIBLE else View.GONE
            repeatFrequencySpinner.setSelection(resources.getStringArray(R.array.repeat_frequency_options).indexOf(it.repeatFrequency ?: ""))
            repeatDaySpinner.setSelection(resources.getStringArray(R.array.days_of_week).indexOf(it.repeatDay ?: ""))
        }
        dateTextView.text = dateFormat.format(selectedDate.time)

        datePickerIcon.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    dateTextView.text = dateFormat.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        repeatToggle.setOnCheckedChangeListener { _, isChecked ->
            repeatFrequencyLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val dialogTitle = if (existingTask == null) R.string.add_task else R.string.edit_task
        val dialog = AlertDialog.Builder(context)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val id = existingTask?.id ?: UUID.randomUUID().toString()
                val date = dateFormat.format(selectedDate.time)
                val description = taskDescription.text.toString()
                val priority = taskPriority.selectedItem.toString()
                val points = taskPoints.text.toString().toIntOrNull() ?: 0
                val assignee = taskAssigneeSpinner.selectedItem.toString()
                val avatar = R.drawable.logo
                val isRepeating = repeatToggle.isChecked
                val repeatFrequency = if (isRepeating) repeatFrequencySpinner.selectedItem.toString() else null
                val repeatDay = if (isRepeating) repeatDaySpinner.selectedItem.toString() else null

                val newTask = DynamicTask(
                    id = id,
                    date = date,
                    description = description,
                    priority = priority,
                    points = points,
                    assignee = assignee,
                    avatar = avatar,
                    isRepeating = isRepeating,
                    repeatFrequency = repeatFrequency,
                    repeatDay = repeatDay
                )

                if (existingTask == null) {
                    wochenplanViewModel.addTask(newTask)
                } else {
                    wochenplanViewModel.updateTask(newTask)
                }
                displayTasksForDay(date)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
    }

    private fun displayTasksForDay(day: String) {
        val dayTasks = wochenplanViewModel.tasks.value?.filter {
            it.date == day || (it.isRepeating && shouldTaskBeShownToday(it, day))
        } ?: emptyList()

        val layout = binding.taskLayout
        layout.removeAllViews()

        for (task in dayTasks) {
            val taskView = LayoutInflater.from(requireContext()).inflate(R.layout.wochenplan_task_item_new, layout, false)
            val descriptionTextView = taskView.findViewById<TextView>(R.id.taskDescription)
            val priorityTextView = taskView.findViewById<TextView>(R.id.taskPriority)
            val pointsTextView = taskView.findViewById<TextView>(R.id.taskPoints)
            val assigneeTextView = taskView.findViewById<TextView>(R.id.taskAsignee)
            val assigneeAvatarImageView = taskView.findViewById<ImageView>(R.id.taskAssigneeAvatar)
            val taskOptionsImageView = taskView.findViewById<ImageView>(R.id.taskOptions)

            descriptionTextView.text = task.description
            priorityTextView.text = task.priority
            pointsTextView.text = "${task.points} Punkte"
            assigneeTextView.text = task.assignee
            assigneeAvatarImageView.setImageResource(task.avatar)

            when (task.priority) {
                "Hoch" -> priorityTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.priority_high))
                "Mittel" -> priorityTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.priority_medium))
                "Niedrig" -> priorityTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.priority_low))
            }

            if (task.isDone) {
                descriptionTextView.paintFlags = descriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }

            // Set click listener for task options icon to show task options dialog
            taskOptionsImageView.setOnClickListener {
                showTaskOptions(task)
            }

            layout.addView(taskView)
        }
    }

    private fun showTaskOptions(task: DynamicTask) {
        val options = if (task.isDone) {
            arrayOf("Nicht erledigt", "Bearbeiten", "Löschen")
        } else {
            arrayOf("Erledigt", "Bearbeiten", "Löschen")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.task_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (task.isDone) {
                            markTaskAsNotDone(task)
                        } else {
                            markTaskAsDone(task)
                        }
                    }
                    1 -> showTaskDialog(task)
                    2 -> deleteTask(task)
                }
            }
            .create()

        dialog.show()
    }

    private fun markTaskAsDone(task: DynamicTask) {
        val updatedTask = task.copy(isDone = true)
        wochenplanViewModel.updateTask(updatedTask)
    }

    private fun markTaskAsNotDone(task: DynamicTask) {
        val updatedTask = task.copy(isDone = false)
        wochenplanViewModel.updateTask(updatedTask)
    }

    private fun deleteTask(task: DynamicTask) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_task_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                wochenplanViewModel.deleteTask(task)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    private fun shouldTaskBeShownToday(task: DynamicTask, day: String): Boolean {
        val calendar = Calendar.getInstance()
        val selectedDate = dateFormat.parse(day)

        return when (task.repeatFrequency) {
            "Wöchentlich" -> {
                val taskDate = dateFormat.parse(task.date)
                taskDate?.let {
                    calendar.time = it
                    val taskDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                    calendar.time = selectedDate ?: Date()
                    val selectedDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    taskDayOfWeek == selectedDayOfWeek
                } ?: false
            }
            "Täglich" -> true
            "Monatlich" -> {
                val taskDate = dateFormat.parse(task.date)
                taskDate?.let {
                    calendar.time = it
                    val taskDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

                    calendar.time = selectedDate ?: Date()
                    val selectedDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
                    taskDayOfMonth == selectedDayOfMonth
                } ?: false
            }
            else -> false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
