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
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId).collection("Haushaltsbuch")
                                .addSnapshotListener { snapshot, error ->
                                    if (error != null) {
                                        Log.e("HaushaltsbuchViewModel", "Error fetching updates: ${error.message}")
                                        return@addSnapshotListener
                                    }
                                    val expenses = snapshot?.documents?.mapNotNull { it.toObject(Expense::class.java) }
                                        ?: listOf()
                                    _allExpenses.value = expenses
                                    loadExpensesForDate(formatDate(selectedDate))
                                }
                        } else {
                            Log.e("HaushaltsbuchViewModel", "WG-ID is null")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("HaushaltsbuchViewModel", "Error fetching WG-ID: ${e.message}")
                    }
            }
        }
    }


    fun addExpenseToFirestore(expense: Expense) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId).collection("Haushaltsbuch").add(expense)
                                .addOnSuccessListener { Log.d("HaushaltsbuchViewModel", "Transaction added successfully.") }
                                .addOnFailureListener { Log.e("HaushaltsbuchViewModel", "Error adding transaction.") }
                        }
                    }
            }
        }
    }

    fun updateExpenseInFirestore(expense: Expense) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId).collection("Haushaltsbuch")
                                .whereEqualTo("datum", expense.datum)
                                .whereEqualTo("kategorie", expense.kategorie)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    snapshot.documents.firstOrNull()?.reference?.set(expense)
                                }
                        }
                    }
            }
        }
    }

    fun deleteExpenseFromFirestore(expense: Expense) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId).collection("Haushaltsbuch")
                                .whereEqualTo("datum", expense.datum)
                                .whereEqualTo("kategorie", expense.kategorie)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    snapshot.documents.forEach { it.reference.delete() }
                                }
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

    private fun formatDate(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    fun loadExpensesForDate(date: String) {
        viewModelScope.launch {
            val expensesForDate = _allExpenses.value?.filter { it.datum == date } ?: emptyList()
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
