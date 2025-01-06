package com.serenitysystems.livable.ui.haushaltsbuch

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HaushaltsbuchViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val userPreferences: UserPreferences = UserPreferences(application)

    private val _allExpenses = MutableLiveData<List<Expense>>(listOf())
    val allExpenses: LiveData<List<Expense>> get() = _allExpenses

    private val _selectedDateExpenses = MutableLiveData<List<Expense>>()
    val selectedDateExpenses: LiveData<List<Expense>> get() = _selectedDateExpenses

    private val _kontostand = MutableLiveData<Float>(0f)
    val kontostand: LiveData<Float> get() = _kontostand

    val selectedDate: Calendar = Calendar.getInstance()

    val categories = listOf(
        "Haushalt", "Lebensmittel", "Gesundheit", "Kleidung", "Freizeit",
        "Transport", "Versicherung", "Bildung", "Unterhaltung", "Reisen", "Sonstiges"
    )

    init {
        listenForRealtimeUpdates()
    }

    private fun listenForRealtimeUpdates() {
        Log.d("HaushaltsbuchViewModel", "Starting to listen for real-time updates")
        fetchUserToken { token ->
            if (token == null) {
                Log.e("HaushaltsbuchViewModel", "User token is null")
                return@fetchUserToken
            }
            val userEmail = token.email
            Log.d("HaushaltsbuchViewModel", "User email: $userEmail")

            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    val wgId = document.getString("wgId")
                    if (wgId == null) {
                        Log.e("HaushaltsbuchViewModel", "WG-ID is null")
                        return@addOnSuccessListener
                    }
                    Log.d("HaushaltsbuchViewModel", "WG-ID: $wgId")

                    db.collection("WGs").document(wgId).collection("Haushaltsbuch")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.e("HaushaltsbuchViewModel", "Error fetching updates: ${error.message}")
                                return@addSnapshotListener
                            }
                            val expenses = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Expense::class.java)?.copy(id = doc.id)
                            } ?: listOf()
                            Log.d("HaushaltsbuchViewModel", "Fetched ${expenses.size} expenses")
                            _allExpenses.value = expenses
                            loadExpensesForDate(formatDate(selectedDate))
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("HaushaltsbuchViewModel", "Error fetching WG-ID: ${e.message}")
                }
        }
    }

    fun addExpenseToFirestore(expense: Expense) {
        Log.d("HaushaltsbuchViewModel", "Adding expense: $expense")
        fetchUserToken { token ->
            if (token == null) {
                Log.e("HaushaltsbuchViewModel", "User token is null")
                return@fetchUserToken
            }
            val userEmail = token.email
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    val wgId = document.getString("wgId")
                    if (wgId == null) {
                        Log.e("HaushaltsbuchViewModel", "WG-ID is null")
                        return@addOnSuccessListener
                    }
                    val docRef = db.collection("WGs").document(wgId).collection("Haushaltsbuch").document()
                    val expenseWithId = expense.copy(id = docRef.id)
                    docRef.set(expenseWithId)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Transaction added with ID: ${docRef.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error adding transaction: ${e.message}")
                        }
                }
        }
    }

    fun updateExpenseInFirestore(expense: Expense) {
        Log.d("HaushaltsbuchViewModel", "Updating expense with ID: ${expense.id}")
        fetchUserToken { token ->
            if (token == null) {
                Log.e("HaushaltsbuchViewModel", "User token is null")
                return@fetchUserToken
            }
            val userEmail = token.email
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    val wgId = document.getString("wgId")
                    if (wgId == null || expense.id.isEmpty()) {
                        Log.e("HaushaltsbuchViewModel", "WG-ID is null or expense ID is empty")
                        return@addOnSuccessListener
                    }
                    db.collection("WGs").document(wgId).collection("Haushaltsbuch")
                        .document(expense.id)
                        .set(expense)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Transaction updated with ID: ${expense.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error updating transaction: ${e.message}")
                        }
                }
        }
    }

    fun deleteExpenseFromFirestore(expense: Expense) {
        Log.d("HaushaltsbuchViewModel", "Deleting expense with ID: ${expense.id}")
        fetchUserToken { token ->
            if (token == null) {
                Log.e("HaushaltsbuchViewModel", "User token is null")
                return@fetchUserToken
            }
            val userEmail = token.email
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    val wgId = document.getString("wgId")
                    if (wgId == null || expense.id.isEmpty()) {
                        Log.e("HaushaltsbuchViewModel", "WG-ID is null or expense ID is empty")
                        return@addOnSuccessListener
                    }
                    db.collection("WGs").document(wgId).collection("Haushaltsbuch")
                        .document(expense.id)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("Firestore", "Transaction deleted with ID: ${expense.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error deleting transaction: ${e.message}")
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

    private fun formatDate(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun loadExpensesForDate(date: String) {
        Log.d("HaushaltsbuchViewModel", "Loading expenses for date: $date")
        viewModelScope.launch {
            val expensesForDate = _allExpenses.value?.filter { it.datum == date } ?: emptyList()
            Log.d("HaushaltsbuchViewModel", "Found ${expensesForDate.size} expenses for date: $date")
            _selectedDateExpenses.value = expensesForDate
            updateTotals()
        }
    }

    private fun updateTotals() {
        val totalIncome = _selectedDateExpenses.value?.filter { it.istEinnahme }
            ?.sumOf { it.betrag.toDouble() }?.toFloat() ?: 0f
        val totalExpense = _selectedDateExpenses.value?.filter { !it.istEinnahme }
            ?.sumOf { it.betrag.toDouble() }?.toFloat() ?: 0f
        _kontostand.value = totalIncome - totalExpense
        Log.d("HaushaltsbuchViewModel", "Total income: $totalIncome, total expense: $totalExpense, balance: ${_kontostand.value}")
    }

    fun getCategoryColor(category: String): Int {
        return when (category) {
            "Haushalt" -> 0xFFFF5722.toInt() // Orange
            "Lebensmittel" -> 0xFF4CAF50.toInt() // Grün
            "Gesundheit" -> 0xFF03A9F4.toInt() // Blau
            "Kleidung" -> 0xFF9C27B0.toInt() // Violett
            "Freizeit" -> 0xFFFFEB3B.toInt() // Gelb
            "Transport" -> 0xFF009688.toInt() // Türkis
            "Versicherung" -> 0xFFFFC107.toInt() // Hellorange
            "Bildung" -> 0xFF673AB7.toInt() // Dunkelviolett
            "Unterhaltung" -> 0xFFE91E63.toInt() // Pink
            "Reisen" -> 0xFF8BC34A.toInt() // Hellgrün
            "Sonstiges" -> 0xFF607D8B.toInt() // Grau
            else -> 0xFF000000.toInt() // Schwarz
        }
    }
}
