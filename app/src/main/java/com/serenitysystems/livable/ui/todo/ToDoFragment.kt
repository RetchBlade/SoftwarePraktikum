package com.serenitysystems.livable.ui.todo

import android.app.DatePickerDialog
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentTodoBinding
import com.serenitysystems.livable.ui.todo.data.TodoItem
import com.serenitysystems.livable.ui.todo.adapter.TodoAdapter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ToDoFragment : Fragment() {

    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!
    private lateinit var todoViewModel: ToDoViewModel
    private lateinit var todayAdapter: TodoAdapter
    private lateinit var tomorrowAdapter: TodoAdapter
    private lateinit var weekAdapter: TodoAdapter
    private lateinit var laterAdapter: TodoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        todoViewModel = ViewModelProvider(this).get(ToDoViewModel::class.java)
        _binding = FragmentTodoBinding.inflate(inflater, container, false)

        todayAdapter = TodoAdapter { todo -> /* Leerer Lambda, keine Aktion mehr */ }
        tomorrowAdapter = TodoAdapter { todo -> /* Leerer Lambda, keine Aktion mehr */ }
        weekAdapter = TodoAdapter { todo -> /* Leerer Lambda, keine Aktion mehr */ }
        laterAdapter = TodoAdapter { todo -> /* Leerer Lambda, keine Aktion mehr */ }

        setupRecyclerViews()

        todoViewModel.todos.observe(viewLifecycleOwner) { todos ->
            sortTodosByDate(todos)
        }

        binding.newTaskButton.setOnClickListener {
            showAddTodoDialog()
        }

        return binding.root
    }

    private fun setupRecyclerViews() {
        binding.todoListRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = todayAdapter
        }
        binding.todoListRecyclerView2.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = tomorrowAdapter
        }
        binding.todoListRecyclerView3.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = weekAdapter
        }
        binding.todoListRecyclerView4.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = laterAdapter
        }

        // Swipe-to-delete für alle RecyclerViews einrichten
        setupSwipeGesture(binding.todoListRecyclerView, todayAdapter)
        setupSwipeGesture(binding.todoListRecyclerView2, tomorrowAdapter)
        setupSwipeGesture(binding.todoListRecyclerView3, weekAdapter)
        setupSwipeGesture(binding.todoListRecyclerView4, laterAdapter)
    }

    private fun setupSwipeGesture(recyclerView: RecyclerView, adapter: TodoAdapter) {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // Keine Move-Aktion, nur Swipe
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // To-Do beim Wischen entfernen
                val position = viewHolder.adapterPosition
                val todo = adapter.currentList[position]
                todoViewModel.deleteTodo(todo)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }



    private fun sortTodosByDate(todos: List<TodoItem>) {
        val sortedTodos = todos.sortedWith(compareByDescending<TodoItem> { getPriorityValue(it.priority) }
            .thenBy { it.date })

        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val weekEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }

        val todayTodos = sortedTodos.filter { isSameDay(it.date, today.time) }
        val tomorrowTodos = sortedTodos.filter { isSameDay(it.date, tomorrow.time) }
        val weekTodos = sortedTodos.filter { it.date.after(tomorrow.time) && it.date.before(weekEnd.time) }
        val laterTodos = sortedTodos.filter { it.date.after(weekEnd.time) }

        todayAdapter.submitList(todayTodos)
        tomorrowAdapter.submitList(tomorrowTodos)
        weekAdapter.submitList(weekTodos)
        laterAdapter.submitList(laterTodos)
    }

    private fun getPriorityValue(priority: String): Int {
        return when (priority) {
            "Hoch" -> 3
            "Mittel" -> 2
            "Niedrig" -> 1
            else -> 0
        }
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val calendar1 = Calendar.getInstance().apply { time = date1 }
        val calendar2 = Calendar.getInstance().apply { time = date2 }
        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) &&
                calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }

    private fun showAddTodoDialog() {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.todo_dialog_add_todo, null)
        val todoDescription: EditText = dialogView.findViewById(R.id.todoDescription)
        val todoDetailedDescription: EditText = dialogView.findViewById(R.id.todoDetailedDescription)
        val datePickerIcon: ImageView = dialogView.findViewById(R.id.datePickerIcon)
        val dateTextView: TextView = dialogView.findViewById(R.id.dateTextView)
        val prioritySpinner: Spinner = dialogView.findViewById(R.id.prioritySpinner)

        val calendar = Calendar.getInstance()
        dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)

        datePickerIcon.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Todo hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val description = todoDescription.text.toString()
                val detailedDescription = todoDetailedDescription.text.toString()
                val priority = prioritySpinner.selectedItem.toString()
                val newTodo = TodoItem(
                    description = description,
                    detailedDescription = detailedDescription,
                    date = calendar.time,
                    priority = priority
                )

                todoViewModel.addTodo(newTodo)
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        dialog.show()
    }


    private fun showTodoOptions(todo: TodoItem) {
        // Optionen für das Todo anzeigen: "Erledigt", "Bearbeiten", "Löschen"
        val options = arrayOf("Erledigt", "Bearbeiten", "Löschen")

        AlertDialog.Builder(requireContext())
            .setTitle("Todo Optionen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> todoViewModel.updateTodo(todo.copy(isDone = true)) // Todo als erledigt markieren
                    1 -> editTodo(todo) // Todo bearbeiten
                    2 -> todoViewModel.deleteTodo(todo) // Todo löschen
                }
            }
            .show()
    }

    private fun editTodo(todo: TodoItem) {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.todo_dialog_add_todo, null)
        val todoDescription: EditText = dialogView.findViewById(R.id.todoDescription)
        val todoDetailedDescription: EditText = dialogView.findViewById(R.id.todoDetailedDescription)
        val datePickerIcon: ImageView = dialogView.findViewById(R.id.datePickerIcon)
        val dateTextView: TextView = dialogView.findViewById(R.id.dateTextView)

        // Set initial data for editing
        todoDescription.setText(todo.description)
        todoDetailedDescription.setText(todo.detailedDescription)
        val calendar = Calendar.getInstance().apply { time = todo.date }
        dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(todo.date)

        // DatePicker öffnet sich beim Klick auf den Icon
        datePickerIcon.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dateTextView.text = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Todo bearbeiten")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val updatedDescription = todoDescription.text.toString()
                val updatedDetailedDescription = todoDetailedDescription.text.toString()
                val updatedTodo = todo.copy(
                    description = updatedDescription,
                    detailedDescription = updatedDetailedDescription,
                    date = calendar.time
                )

                todoViewModel.updateTodo(updatedTodo)
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

