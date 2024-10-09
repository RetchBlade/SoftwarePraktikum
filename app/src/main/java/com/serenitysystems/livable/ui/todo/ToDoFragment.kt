package com.serenitysystems.livable.ui.todo

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.serenitysystems.livable.R
import com.serenitysystems.livable.databinding.FragmentTodoBinding
import com.serenitysystems.livable.ui.todo.data.TodoItem
import java.util.Calendar

class ToDoFragment : Fragment() {
    private var _binding: FragmentTodoBinding? = null
    private val binding get() = _binding!!
    private lateinit var todoViewModel: ToDoViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        todoViewModel = ViewModelProvider(this).get(ToDoViewModel::class.java)
        _binding = FragmentTodoBinding.inflate(inflater, container, false)

        todoViewModel.todos.observe(viewLifecycleOwner) { todos ->
            displayTodos(todos)
        }

        binding.addTodoButton.setOnClickListener {
            showAddTodoDialog()
        }

        return binding.root
    }

    private fun displayTodos(todos: List<TodoItem>) {
        val layout = binding.todoLayout
        layout.removeAllViews()

        // Filter für heute, morgen und diese Woche
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val weekEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }

        val todayTodos = todos.filter { it.date.before(tomorrow.time) }
        val tomorrowTodos = todos.filter { it.date.before(weekEnd.time) && it.date.after(today.time) }
        val weekTodos = todos.filter { it.date.after(tomorrow.time) }

        // "Heute" anzeigen
        layout.addView(createHeaderView("Heute"))
        todayTodos.forEach { layout.addView(createTodoView(it)) }

        // "Morgen" anzeigen
        layout.addView(createHeaderView("Morgen"))
        tomorrowTodos.forEach { layout.addView(createTodoView(it)) }

        // "Diese Woche" anzeigen
        layout.addView(createHeaderView("Diese Woche"))
        weekTodos.forEach { layout.addView(createTodoView(it)) }
    }

    private fun showAddTodoDialog() {
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.todo_dialog_add_todo, null)
        val todoDescription: EditText = dialogView.findViewById(R.id.todoDescription)
        val todoDatePicker: DatePicker = dialogView.findViewById(R.id.todoDatePicker)

        val dialog = AlertDialog.Builder(context)
            .setTitle("Todo hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val description = todoDescription.text.toString()
                val calendar = Calendar.getInstance()
                calendar.set(todoDatePicker.year, todoDatePicker.month, todoDatePicker.dayOfMonth)
                val newTodo = TodoItem(description = description, date = calendar.time)

                todoViewModel.addTodo(newTodo)
            }
            .setNegativeButton("Abbrechen", null)
            .create()

        dialog.show()
    }

    private fun createHeaderView(title: String): View {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.todo_header_item, null)
        view.findViewById<TextView>(R.id.headerTitle).text = title
        return view
    }

    private fun createTodoView(todo: TodoItem): View {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.todo_item, null)
        val descriptionView = view.findViewById<TextView>(R.id.todoDescription)
        descriptionView.text = todo.description

        if (todo.isDone) {
            descriptionView.paintFlags = descriptionView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }

        view.setOnClickListener {
            // Todo Optionen (erledigt, bearbeiten, löschen) anzeigen
            showTodoOptions(todo)
        }

        return view
    }

    private fun showTodoOptions(todo: TodoItem) {
        val options = arrayOf("Erledigt", "Bearbeiten", "Löschen")

        AlertDialog.Builder(requireContext())
            .setTitle("Todo Optionen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> todoViewModel.updateTodo(todo.copy(isDone = true))
                    1 -> editTodo(todo)
                    2 -> todoViewModel.deleteTodo(todo)
                }
            }
            .show()
    }

    private fun editTodo(todo: TodoItem) {
        // Bearbeitungsdialog anzeigen muss noch implementiert werden
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
