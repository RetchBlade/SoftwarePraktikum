package com.serenitysystems.livable.ui.haushaltsbuch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HaushaltsbuchViewModel : ViewModel() {

    private val _allExpenses = MutableLiveData<List<Expense>>().apply { value = mutableListOf() }
    val allExpenses: LiveData<List<Expense>> get() = _allExpenses

    private val _kontostand = MutableLiveData<Float>().apply { value = 0f }
    val kontostand: LiveData<Float> get() = _kontostand

    private val _totalAusgabe = MutableLiveData<Float>().apply { value = 0f }
    val totalAusgabe: LiveData<Float> get() = _totalAusgabe

    private val _totalEinnahmen = MutableLiveData<Float>().apply { value = 0f }
    val totalEinnahmen: LiveData<Float> get() = _totalEinnahmen

    private val _selectedDateExpenses = MutableLiveData<List<Expense>>()
    val selectedDateExpenses: LiveData<List<Expense>> get() = _selectedDateExpenses

    private val categories = listOf(
        "Haushalt",
        "Lebensmittel",
        "Gesundheit",
        "Kleidung",
        "Freizeit",
        "Transport",
        "Versicherung",
        "Bildung",
        "Unterhaltung",
        "Reisen",
        "Sonstiges"
    )

    fun addExpense(expense: Expense) {
        val currentList = _allExpenses.value?.toMutableList() ?: mutableListOf()
        currentList.add(expense)
        _allExpenses.value = currentList
        calculateTotals()
        loadExpensesForDate(Calendar.getInstance())
    }

    fun editExpense(index: Int, updatedExpense: Expense) {
        val currentList = _allExpenses.value?.toMutableList() ?: mutableListOf()
        if (index in currentList.indices) {
            currentList[index] = updatedExpense
            _allExpenses.value = currentList
            calculateTotals()
            loadExpensesForDate(Calendar.getInstance())
        }
    }

    fun loadExpensesForDate(date: Calendar) {
        val dateString = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date.time)
        val expensesForDate = _allExpenses.value?.filter { it.datum == dateString && !it.isDeleted } ?: emptyList()
        _selectedDateExpenses.value = expensesForDate
        calculateTotals()
    }

    fun clearExpensesForDate(date: Calendar) {
        val dateString = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(date.time)
        _allExpenses.value = _allExpenses.value?.filter { it.datum != dateString }
        calculateTotals()
    }

    private fun calculateTotals() {
        val einnahmenTotal = _allExpenses.value?.filter { it.istEinnahme && !it.isDeleted }?.sumByDouble { it.betrag.toDouble() }?.toFloat() ?: 0f
        val ausgabenTotal = _allExpenses.value?.filter { !it.istEinnahme && !it.isDeleted }?.sumByDouble { it.betrag.toDouble() }?.toFloat() ?: 0f
        _totalEinnahmen.value = einnahmenTotal
        _totalAusgabe.value = ausgabenTotal
        _kontostand.value = einnahmenTotal - ausgabenTotal
    }

    fun getCategories(): List<String> = categories

    fun getCategoryColor(category: String): String {
        return when (category) {
            "Haushalt" -> "#FF5722"
            "Lebensmittel" -> "#4CAF50"
            "Gesundheit" -> "#03A9F4"
            "Kleidung" -> "#9C27B0"
            "Freizeit" -> "#FFEB3B"
            "Transport" -> "#009688"
            "Versicherung" -> "#FFC107"
            "Bildung" -> "#673AB7"
            "Unterhaltung" -> "#E91E63"
            "Reisen" -> "#8BC34A"
            "Sonstiges" -> "#607D8B"
            else -> "#000000"
        }
    }

    fun getCategoryTotal(category: String): Float {
        return _allExpenses.value?.filter { it.kategorie == category && !it.istEinnahme }?.sumByDouble { it.betrag.toDouble() }?.toFloat() ?: 0f
    }

    fun getCategoryPercentage(category: String): Float {
        val totalForCategory = getCategoryTotal(category)
        val totalAusgabe = _totalAusgabe.value ?: 1f
        return if (totalAusgabe != 0f) (totalForCategory / totalAusgabe) * 100 else 0f
    }
}
