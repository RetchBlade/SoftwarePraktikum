package com.serenitysystems.livable.ui.wochenplan

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentWochenplanBinding
import com.serenitysystems.livable.ui.wochenplan.data.Task
import java.util.*

class WochenplanFragment : Fragment() {
    private var _binding: FragmentWochenplanBinding? = null
    private val binding get() = _binding!!
    private lateinit var wochenplanViewModel: WochenplanViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        wochenplanViewModel = ViewModelProvider(this).get(WochenplanViewModel::class.java)

        _binding = FragmentWochenplanBinding.inflate(inflater, container, false)
        val root: View = binding.root

        wochenplanViewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            displayTasks(tasks)
        }

        binding.addTaskButton.setOnClickListener {
            showAddTaskDialog()
        }

        return root
    }

    private fun showAddTaskDialog() {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.wochenplan_dialog_add_task, null)
        val daySpinner: Spinner = dialogView.findViewById(R.id.daySpinner)
        val taskDescription: EditText = dialogView.findViewById(R.id.taskDescription)
        val taskPriority: Spinner = dialogView.findViewById(R.id.taskPriority)
        val taskPoints: EditText = dialogView.findViewById(R.id.taskPoints)
        val taskAssignee: EditText = dialogView.findViewById(R.id.taskAssignee)

        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.add_task)
            .setView(dialogView)
            .setPositiveButton(R.string.add_task) { _, _ ->
                val id = UUID.randomUUID().toString()
                val day = daySpinner.selectedItem.toString()
                val description = taskDescription.text.toString()
                val priority = taskPriority.selectedItem.toString()
                val points = taskPoints.text.toString().toIntOrNull() ?: 0
                val assignee = taskAssignee.text.toString()
                val avatar = R.drawable.logo // Fester Avatar für den Anfang

                val newTask = Task(id,day, description, priority, points, assignee, avatar)
                wochenplanViewModel.addTask(newTask)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
    }

    private fun displayTasks(tasks: List<Task>) {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val daysOfWeek = arrayOf("Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag")
        val startIndex = today - 1

        val layout = binding.taskLayout
        layout.removeAllViews()

        for (i in 0..4) {
            val dayIndex = (startIndex + i) % 7
            val day = daysOfWeek[dayIndex]

            val dayTasks = tasks.filter { it.day == day }

            val dayHeader = LayoutInflater.from(requireContext()).inflate(R.layout.wochenplan_day_header, layout, false)
            dayHeader.findViewById<TextView>(R.id.dayTitle).text = day
            layout.addView(dayHeader)

            for (task in dayTasks) {
                val taskView = LayoutInflater.from(requireContext()).inflate(R.layout.wochenplan_task_item, layout, false)
                val descriptionTextView = taskView.findViewById<TextView>(R.id.taskDescription)
                descriptionTextView.text = task.description
                val priorityTextView = taskView.findViewById<TextView>(R.id.taskPriority)
                priorityTextView.text = task.priority

                // Setze die Textfarbe basierend auf der Priorität
                when (task.priority) {
                    "Hoch" -> priorityTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.priority_high))
                    "Mittel" -> priorityTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.priority_medium))
                    "Niedrig" -> priorityTextView.setTextColor(ContextCompat.getColor(requireContext(), R.color.priority_low))
                }

                taskView.findViewById<TextView>(R.id.taskPoints).text = "${task.points} Punkte"
                taskView.findViewById<TextView>(R.id.taskAssignee).text = task.assignee
                taskView.findViewById<ImageView>(R.id.taskAssigneeAvatar).setImageResource(task.avatar)

                // Setze den Text durchgestrichen, wenn die Aufgabe erledigt ist
                if (task.isDone) {
                    descriptionTextView.paintFlags = descriptionTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                }

                taskView.setOnClickListener {
                    showTaskOptions(task)
                }

                layout.addView(taskView)
            }
        }
    }

    private fun showTaskOptions(task: Task) {
        val options = arrayOf("Erledigt", "Bearbeiten", "Löschen")

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.task_options)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> markTaskAsDone(task)
                    1 -> editTask(task)
                    2 -> deleteTask(task)
                }
            }
            .show()
    }

    private fun markTaskAsDone(task: Task) {
            val updatedTask = task.copy(isDone = true)
            wochenplanViewModel.updateTask(updatedTask)
    }

    private fun editTask(task: Task) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.wochenplan_dialog_add_task, null)
        val daySpinner: Spinner = dialogView.findViewById(R.id.daySpinner)
        val taskDescription: EditText = dialogView.findViewById(R.id.taskDescription)
        val taskPriority: Spinner = dialogView.findViewById(R.id.taskPriority)
        val taskPoints: EditText = dialogView.findViewById(R.id.taskPoints)
        val taskAssignee: EditText = dialogView.findViewById(R.id.taskAssignee)

        // Setze die aktuellen Werte in den EditText und Spinner
        daySpinner.setSelection(resources.getStringArray(R.array.days_of_week).indexOf(task.day))
        taskDescription.setText(task.description)
        taskPriority.setSelection(resources.getStringArray(R.array.prio_liste).indexOf(task.priority))
        taskPoints.setText(task.points.toString())
        taskAssignee.setText(task.assignee)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.edit_task)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val updatedTask = task.copy(
                    day = daySpinner.selectedItem.toString(),
                    description = taskDescription.text.toString(),
                    priority = taskPriority.selectedItem.toString(),
                    points = taskPoints.text.toString().toIntOrNull() ?: task.points,
                    assignee = taskAssignee.text.toString()
                )
                wochenplanViewModel.updateTask(updatedTask)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
    }

    private fun deleteTask(task: Task) {
        AlertDialog.Builder(requireContext())
            .setTitle("delete_task")
            .setMessage(R.string.delete_task_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                wochenplanViewModel.deleteTask(task)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
