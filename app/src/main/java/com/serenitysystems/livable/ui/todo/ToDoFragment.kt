package com.serenitysystems.livable.ui.todo

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.serenitysystems.livable.ui.todo.adapter.TodoAdapter
import com.serenitysystems.livable.ui.todo.data.TodoItem
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

        todayAdapter = TodoAdapter { todo -> handleTodoAction(todo) }
        tomorrowAdapter = TodoAdapter { todo -> handleTodoAction(todo) }
        weekAdapter = TodoAdapter { todo -> handleTodoAction(todo) }
        laterAdapter = TodoAdapter { todo -> handleTodoAction(todo) }

        setupRecyclerViews()

        todoViewModel.todos.observe(viewLifecycleOwner) { todos ->
            sortTodosByDate(todos)
        }

        binding.newTaskButton.setOnClickListener {
            showAddTodoDialog()
        }

        return binding.root
    }

    private fun handleTodoAction(todo: TodoItem) {
        if (todo.detailedDescription == "deleted_by_button") {
            // Permanentes Löschen durch den Müll-Knopf
            todoViewModel.deleteTodoPermanently(todo)
        } else {
            // Abhaken oder Entabhaken
            todoViewModel.updateTodo(todo)
        }
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
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val todo = adapter.currentList[position]
                todoViewModel.deleteTodo(todo) // Standard Swipe-Logik
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun sortTodosByDate(todos: List<TodoItem>) {
        val sortedTodos = todos.sortedWith(
            compareByDescending<TodoItem> { getPriorityValue(it.priority) }
                .thenBy { it.date }
        )

        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY

        val today = calendar.clone() as Calendar
        val tomorrow = calendar.clone() as Calendar
        tomorrow.add(Calendar.DAY_OF_YEAR, 1)

        val weekEnd = calendar.clone() as Calendar
        weekEnd.set(Calendar.DAY_OF_WEEK, weekEnd.firstDayOfWeek + 6)

        val todayTodos = mutableListOf<TodoItem>()
        val tomorrowTodos = mutableListOf<TodoItem>()
        val weekTodos = mutableListOf<TodoItem>()
        val laterTodos = mutableListOf<TodoItem>()

        for (todo in sortedTodos) {
            when {
                isSameDay(todo.date, today.time) -> todayTodos.add(todo)
                isSameDay(todo.date, tomorrow.time) -> tomorrowTodos.add(todo)
                todo.date.after(tomorrow.time) && todo.date.before(weekEnd.time) -> weekTodos.add(todo)
                else -> laterTodos.add(todo)
            }
        }

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
        val repeatTypeSpinner: Spinner = dialogView.findViewById(R.id.repeatTypeSpinner)

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

        AlertDialog.Builder(context)
            .setTitle("Todo hinzufügen")
            .setView(dialogView)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val description = todoDescription.text.toString()
                val detailedDescription = todoDetailedDescription.text.toString()
                val priority = prioritySpinner.selectedItem.toString()
                val repeatType = when (repeatTypeSpinner.selectedItemPosition) {
                    1 -> "daily"
                    2 -> "every_2_days"
                    3 -> "weekly"
                    4 -> "specific_day"
                    else -> null
                }

                val newTodo = TodoItem(
                    description = description,
                    detailedDescription = detailedDescription,
                    date = calendar.time,
                    priority = priority,
                    repeatType = repeatType
                )
                todoViewModel.addTodo(newTodo)
            }
            .setNegativeButton("Abbrechen", null)
            .show()
    }

    private fun wasDeletedByButton(todo: TodoItem): Boolean {
        return todo.detailedDescription == "deleted_by_button"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}