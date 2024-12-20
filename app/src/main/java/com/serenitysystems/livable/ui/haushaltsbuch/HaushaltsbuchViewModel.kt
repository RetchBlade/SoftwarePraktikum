package com.serenitysystems.livable.ui.haushaltsbuch

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.serenitysystems.livable.ui.haushaltsbuch.data.Expense
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HaushaltsbuchViewModel : ViewModel() {

    // Alle Ausgaben und Einnahmen
    private val _allExpenses = MutableLiveData<List<Expense>>(listOf())
    val allExpenses: LiveData<List<Expense>> get() = _allExpenses

    // Ausgaben und Einnahmen für das ausgewählte Datum
    private val _selectedDateExpenses = MutableLiveData<List<Expense>>()
    val selectedDateExpenses: LiveData<List<Expense>> get() = _selectedDateExpenses

    // Aktueller Kontostand
    private val _kontostand = MutableLiveData<Float>(0f)
    val kontostand: LiveData<Float> get() = _kontostand

    // Aktuell ausgewähltes Datum
    val selectedDate: Calendar = Calendar.getInstance()

    // Verfügbare Kategorien
    val categories = listOf(
        "Haushalt", "Lebensmittel", "Gesundheit", "Kleidung", "Freizeit",
        "Transport", "Versicherung", "Bildung", "Unterhaltung", "Reisen", "Sonstiges"
    )

    // Hinzufügen einer neuen Ausgabe oder Einnahme
    fun addExpense(expense: Expense) {
        if (expense.kategorie.isEmpty() || expense.datum.isEmpty() || expense.betrag <= 0) {
            Log.e("ViewModel", "Invalid expense data")
            return
        }
        viewModelScope.launch {
            _allExpenses.value = (_allExpenses.value ?: emptyList()) + expense
            loadExpensesForDateAsync(formatDate(selectedDate))
        }
    }


    // Aktualisieren einer bestehenden Ausgabe oder Einnahme
    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            val currentExpenses = _allExpenses.value ?: listOf()
            val index = currentExpenses.indexOfFirst { it == expense }
            if (index >= 0) {
                val updatedExpenses = currentExpenses.toMutableList()
                updatedExpenses[index] = expense
                _allExpenses.value = updatedExpenses
                loadExpensesForDateAsync(formatDate(selectedDate))
            }
        }
    }

    // Löschen einer Ausgabe oder Einnahme
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            val currentExpenses = _allExpenses.value ?: listOf()
            val updatedExpenses = currentExpenses - expense
            _allExpenses.value = updatedExpenses
            loadExpensesForDateAsync(formatDate(selectedDate))
        }
    }

    // Berechnung des prozentualen Anteils einer Kategorie
    fun getCategoryPercentage(category: String): Float {
        val totalAusgabe = _selectedDateExpenses.value?.filter { !it.istEinnahme }
            ?.sumOf { it.betrag.toDouble() }?.toFloat() ?: 1f
        val categoryTotal = _selectedDateExpenses.value?.filter { it.kategorie == category && !it.istEinnahme }
            ?.sumOf { it.betrag.toDouble() }?.toFloat() ?: 0f
        return if (totalAusgabe != 0f) (categoryTotal / totalAusgabe) * 100 else 0f
    }

    // Berechnung des Betrags einer Kategorie
    fun getCategoryAmount(category: String): Float {
        return _selectedDateExpenses.value?.filter { it.kategorie == category && !it.istEinnahme }
            ?.sumOf { it.betrag.toDouble() }?.toFloat() ?: 0f
    }

    // Farbe für eine Kategorie erhalten
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

    // Aktualisierung des Kontostands
    private fun updateTotals() {
        val totalIncome = _selectedDateExpenses.value?.filter { it.istEinnahme }
            ?.sumOf { it.betrag.toDouble() }?.toFloat() ?: 0f
        val totalExpense = _selectedDateExpenses.value?.filter { !it.istEinnahme }
            ?.sumOf { it.betrag.toDouble() }?.toFloat() ?: 0f
        _kontostand.value = totalIncome - totalExpense
    }

    // Laden der Ausgaben und Einnahmen für das ausgewählte Datum
    fun loadExpensesForDate(date: String) {
        viewModelScope.launch {
            loadExpensesForDateAsync(date)
        }
    }

    private suspend fun loadExpensesForDateAsync(date: String) {
        val expensesForDate = _allExpenses.value?.filter { it.datum == date } ?: emptyList()
        _selectedDateExpenses.value = expensesForDate
        updateTotals()
    }

    // Ändern des ausgewählten Datums um eine bestimmte Anzahl von Tagen
    fun changeDateByDays(days: Int) {
        selectedDate.add(Calendar.DAY_OF_MONTH, days)
        loadExpensesForDate(formatDate(selectedDate))
    }

    // Hilfsfunktion zum Formatieren des Datums
    private fun formatDate(calendar: Calendar): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }
}
