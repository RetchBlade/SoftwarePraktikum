package com.serenitysystems.livable.ui.haushaltsbuch

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class HaushaltsbuchViewModel : ViewModel() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val categories = listOf(
        "Restaurant", "Essen", "Kleidung", "Gesundheit", "Auto", "Andere"
    )
    private val colors = listOf(
        "#FF0000", "#FFA500", "#008000", "#0000FF", "#800080", "#808080"
    )

    private val expensesData = mutableMapOf<String, MutableList<Expense>>()

    private val _selectedDate = MutableLiveData<Date>().apply {
        value = Date()
    }
    val selectedDate: LiveData<Date> = _selectedDate

    private val _expensesForDate = MutableLiveData<List<Expense>>()
    val expensesForDate: LiveData<List<Expense>> = _expensesForDate

    private val _kontostand = MutableLiveData<Float>().apply {
        value = 0f
    }
    val kontostand: LiveData<Float> = _kontostand

    fun addExpense(kategorie: String, betrag: Float, beschreibung: String, istEinnahme: Boolean) {
        val dateKey = dateFormat.format(_selectedDate.value ?: Date())
        val expenseList = expensesData.getOrPut(dateKey) { mutableListOf() }

        val newExpense = Expense(kategorie, betrag, beschreibung, istEinnahme)
        expenseList.add(newExpense)

        if (istEinnahme) {
            _kontostand.value = _kontostand.value?.plus(betrag)
        } else {
            _kontostand.value = _kontostand.value?.minus(betrag)
        }

        updateExpensesForDate(_selectedDate.value ?: Date())
    }

    fun updateExpensesForDate(date: Date) {
        val dateKey = dateFormat.format(date)
        _expensesForDate.value = expensesData[dateKey] ?: emptyList()
    }

    fun changeDate(days: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = _selectedDate.value ?: Date()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        _selectedDate.value = calendar.time
        updateExpensesForDate(calendar.time)
    }

    // Yeni fonksiyon: changeDateTo
    fun changeDateTo(date: Date) {
        _selectedDate.value = date
        updateExpensesForDate(date)
    }

    fun getCategories(): List<String> = categories

    fun getCategoryColor(category: String): String {
        val index = categories.indexOf(category)
        return if (index != -1) colors[index] else "#000000"
    }
}

data class Expense(
    val kategorie: String,
    val betrag: Float,
    val beschreibung: String,
    val istEinnahme: Boolean,
    var isStrikethrough: Boolean = false
)
