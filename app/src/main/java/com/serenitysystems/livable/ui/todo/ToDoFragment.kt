package com.serenitysystems.livable.ui.todo

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.EditText
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
import java.util.Calendar
import java.util.Date

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

        todayAdapter = TodoAdapter { todo -> showTodoOptions(todo) }
        tomorrowAdapter = TodoAdapter { todo -> showTodoOptions(todo) }
        weekAdapter = TodoAdapter { todo -> showTodoOptions(todo) }
        laterAdapter = TodoAdapter { todo -> showTodoOptions(todo) }

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
        val today = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val weekEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }

        // Filter Todos by date
        val todayTodos = todos.filter { isSameDay(it.date, today.time) }
        val tomorrowTodos = todos.filter { isSameDay(it.date, tomorrow.time) }
        val weekTodos = todos.filter { it.date.after(tomorrow.time) && it.date.before(weekEnd.time) }
        val laterTodos = todos.filter { it.date.after(weekEnd.time) }

        // Set data in adapters
        todayAdapter.submitList(todayTodos)
        tomorrowAdapter.submitList(tomorrowTodos)
        weekAdapter.submitList(weekTodos)
        laterAdapter.submitList(laterTodos)
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
        // Logik zum Bearbeiten des Todos hinzufügen (kann ähnlich wie showAddTodoDialog sein)
        val context = requireContext()
        val dialogView = LayoutInflater.from(context).inflate(R.layout.todo_dialog_add_todo, null)
        val todoDescription: EditText = dialogView.findViewById(R.id.todoDescription)
        val todoDatePicker: DatePicker = dialogView.findViewById(R.id.todoDatePicker)

        todoDescription.setText(todo.description)
        val calendar = Calendar.getInstance().apply { time = todo.date }
        todoDatePicker.updateDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

        val dialog = AlertDialog.Builder(context)
            .setTitle("Todo bearbeiten")
            .setView(dialogView)
            .setPositiveButton("Speichern") { _, _ ->
                val updatedDescription = todoDescription.text.toString()
                val updatedCalendar = Calendar.getInstance()
                updatedCalendar.set(todoDatePicker.year, todoDatePicker.month, todoDatePicker.dayOfMonth)
                val updatedTodo = todo.copy(description = updatedDescription, date = updatedCalendar.time)

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

