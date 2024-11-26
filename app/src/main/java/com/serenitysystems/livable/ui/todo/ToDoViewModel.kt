package com.serenitysystems.livable.ui.todo


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.ui.todo.data.TodoItem
import java.util.Calendar

class ToDoViewModel : ViewModel() {
    private val _todos = MutableLiveData<List<TodoItem>>()
    val todos: LiveData<List<TodoItem>> = _todos

    init {
        loadTodos()
    }

    private fun loadTodos() {
        // Beispielhafte Todos (du kannst diese aus einer Datenquelle laden)
        val todosList = listOf(
            TodoItem(description = "Wäsche waschen", detailedDescription =  "", date = Calendar.getInstance().time),
            TodoItem(description = "Einkaufen", detailedDescription =  "", date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time),
            TodoItem(description = "Projekt abschließen", detailedDescription =  "", date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 5) }.time)
        )
        _todos.value = todosList
    }

    fun addTodo(todo: TodoItem) {
        val updatedTodos = (_todos.value.orEmpty() + todo).sortedWith(
            compareByDescending<TodoItem> { getPriorityValue(it.priority) }
                .thenBy { it.date }
        )
        _todos.value = updatedTodos
    }

    private fun getPriorityValue(priority: String): Int {
        return when (priority) {
            "Hoch" -> 3
            "Mittel" -> 2
            "Niedrig" -> 1
            else -> 0
        }
    }

    fun updateTodo(updatedTodo: TodoItem) {
        val updatedTodos = _todos.value?.map { if (it.id == updatedTodo.id) updatedTodo else it }
        _todos.value = updatedTodos
    }

    fun deleteTodo(todo: TodoItem) {
        val updatedTodos = _todos.value.orEmpty().filter { it.id != todo.id }.toMutableList()

        if (todo.repeatType != null) {
            val calendar = Calendar.getInstance().apply { time = todo.date }
            when (todo.repeatType) {
                "daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "every_2_days" -> calendar.add(Calendar.DAY_OF_YEAR, 2)
                "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            val repeatedTodo = todo.copy(date = calendar.time, isDone = false)
            updatedTodos.add(repeatedTodo)
        }

        _todos.value = updatedTodos
    }

    fun deleteTodoPermanently(todo: TodoItem) {
        val updatedTodos = _todos.value.orEmpty().filter { it.id != todo.id }
        _todos.value = updatedTodos
    }


}
