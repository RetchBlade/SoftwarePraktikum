package com.serenitysystems.livable.ui.todo

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import com.serenitysystems.livable.ui.todo.data.TodoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ToDoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val userPreferences: UserPreferences = UserPreferences(application)

    // LiveData oder MutableStateFlow, um Todos zu halten
    private val _todos = MutableLiveData<List<TodoItem>>()
    val todos: LiveData<List<TodoItem>> get() = _todos

    init {
        loadTodos()
    }

    // Funktion, um Todos aus Firestore zu laden
    private fun loadTodos() {
        viewModelScope.launch(Dispatchers.IO) {
            fetchUserToken { token ->
                token?.let { userToken ->
                    val userId = userToken.email
                    val userTodosRef = db.collection("usersammlung")
                        .document(userId)
                        .collection("user_todos")

                    userTodosRef.addSnapshotListener { snapshot, exception ->
                        if (exception != null) {
                            Log.w("Firestore", "Listen failed.", exception)
                            return@addSnapshotListener
                        }

                        val todos = snapshot?.documents?.mapNotNull { document ->
                            if (document.exists()) {
                                document.toObject(TodoItem::class.java)?.copy(id = document.id)
                            } else {
                                null // Ignoriere gelöschte Dokumente
                            }
                        } ?: emptyList()

                        _todos.postValue(todos)
                    }

                }
            }
        }
    }


    private fun fetchUserToken(action: (UserToken?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                action(userToken)
            }
        }
    }

    // Funktion, um ein neues Todo zu Firestore hinzuzufügen
    fun addTodo(todo: TodoItem) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userId = userToken.email
                addTodoToFirestore(todo, userId)
            }
        }
    }


    // Funktion zum Hinzufügen eines Todos zu Firestore
    private fun addTodoToFirestore(todo: TodoItem, userId: String) {

        val userTodosRef = db.collection("usersammlung").document(userId).collection("user_todos")
        val todoData = hashMapOf(
            "description" to todo.description,
            "detailedDescription" to todo.detailedDescription,
            "date" to todo.date,
            "priority" to todo.priority,
            "isDone" to todo.isDone,
            "repeatType" to todo.repeatType // Füge die Wiederholungsinformation hinzu
        )

        userTodosRef.add(todoData)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Todo added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding Todo", e)
            }
    }


    // Funktion zum Bearbeiten eines Todos
    fun updateTodo(todo: TodoItem) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userId = userToken.email
                updateTodoInFirestore(todo, userId)
            }
        }
    }

    // Funktion zum Bearbeiten eines Todos in Firestore
    private fun updateTodoInFirestore(todo: TodoItem, userId: String) {
        val userTodosRef = db.collection("usersammlung").document(userId).collection("user_todos")

        userTodosRef.document(todo.id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    userTodosRef.document(todo.id).set(todo)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Todo updated successfully!")
                        }
                        .addOnFailureListener { e ->
                            Log.w("Firestore", "Error updating Todo", e)
                        }
                } else {
                    Log.d("Firestore", "Todo no longer exists, skipping update.")
                }
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error checking Todo existence", e)
            }
    }


    @Volatile
    private var isProcessing = false

    fun deleteTodo(todo: TodoItem, forceDelete: Boolean = false) {
        if (isProcessing) return // Blockiere parallel laufende Operationen
        isProcessing = true

        fetchUserToken { token ->
            token?.let { userToken ->
                val userId = userToken.email

                if (forceDelete || todo.repeatType == null) {
                    deleteTodoFromFirestore(todo.id, userId)
                } else {
                    val calendar = Calendar.getInstance().apply { time = todo.date }
                    when (todo.repeatType) {
                        "daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                        "every_2_days" -> calendar.add(Calendar.DAY_OF_YEAR, 2)
                        "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                    val updatedTodo = todo.copy(date = calendar.time, isDone = false)
                    updateTodoInFirestore(updatedTodo, userId)
                }
            }
            isProcessing = false
        }
    }

    // Funktion zum Löschen eines Todos in Firestore
    private fun deleteTodoFromFirestore(todoId: String, userId: String) {
        val userTodosRef = db.collection("usersammlung").document(userId).collection("user_todos")
        userTodosRef.document(todoId).delete()
            .addOnSuccessListener {
                Log.d("Firestore", "Todo successfully deleted!")
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error deleting Todo", e)
            }
    }
}
