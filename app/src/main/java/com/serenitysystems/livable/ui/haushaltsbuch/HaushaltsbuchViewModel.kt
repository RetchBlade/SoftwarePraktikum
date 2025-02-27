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
import java.util.Locale
import java.util.Calendar

/**
 * ViewModel zur Verwaltung des Haushaltsbuchs (Einnahmen und Ausgaben).
 * Bietet:
 * - Realtime-Updates aus Firestore
 * - Tages- und Monatsfilter
 * - Kontostandberechnung
 */
class HaushaltsbuchViewModel(application: Application) : AndroidViewModel(application) {

    // Firestore-Datenbank
    private val db = FirebaseFirestore.getInstance()

    // Um User-Token, WG-ID etc. auszulesen
    private val userPreferences: UserPreferences = UserPreferences(application)

    /**
     * Enthält ALLE Einträge aus der Firestore-Sammlung "Haushaltsbuch" der jeweiligen WG.
     * Wird durch addSnapshotListener in listenForRealtimeUpdates() befüllt.
     */
    private val _allExpenses = MutableLiveData<List<Expense>>(listOf())
    val allExpenses: LiveData<List<Expense>> get() = _allExpenses

    /**
     * Entweder tagesgenau oder monatsgenau gefilterte Liste, abhängig vom zuletzt
     * aufgerufenen loadExpensesForDate(...) oder loadExpensesForMonth(...).
     */
    private val _selectedDateExpenses = MutableLiveData<List<Expense>>()
    val selectedDateExpenses: LiveData<List<Expense>> get() = _selectedDateExpenses

    /**
     * Kontostand (Einnahmen - Ausgaben) bezogen auf _selectedDateExpenses.
     * Aktualisiert sich in updateTotals().
     */
    private val _kontostand = MutableLiveData<Float>(0f)
    val kontostand: LiveData<Float> get() = _kontostand

    // Falls man im ViewModel ein ausgewähltes Datum halten möchte (optional).
    val selectedDate: Calendar = Calendar.getInstance()

    /**
     * Beispielkategorien. In vielen Apps könnte dies dynamisch verwaltet werden.
     */
    val categories = listOf(
        "Haushalt", "Lebensmittel", "Gesundheit", "Kleidung", "Freizeit",
        "Transport", "Versicherung", "Bildung", "Unterhaltung", "Reisen", "Sonstiges"
    )

    init {
        // Starte direkt beim ViewModel-Start das Echtzeit-Lauschen auf Firestore
        listenForRealtimeUpdates()
    }

    /**
     * Lauscht auf Änderungen in Firestore (WG-spezifisch). Bei jeder Änderung
     * laden wir sämtliche Einträge in _allExpenses. Falls schon ein Datum gewählt war,
     * wird die gefilterte Liste (z. B. tägliche Ansicht) neu aktualisiert.
     */
    private fun listenForRealtimeUpdates() {
        Log.d("HaushaltsbuchViewModel", "Starte Echtzeit-Listener (SnapshotListener)")
        fetchUserToken { token ->
            if (token == null) {
                Log.e("HaushaltsbuchViewModel", "User token ist null, kann nicht fortfahren.")
                return@fetchUserToken
            }
            val userEmail = token.email ?: return@fetchUserToken

            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { document ->
                    val wgId = document.getString("wgId")
                    if (wgId == null) {
                        Log.e("HaushaltsbuchViewModel", "WG-ID ist null, Abbruch.")
                        return@addOnSuccessListener
                    }
                    db.collection("WGs").document(wgId).collection("Haushaltsbuch")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                Log.e("HaushaltsbuchViewModel", "Fehler beim Lesen: ${error.message}")
                                return@addSnapshotListener
                            }
                            val expenses = snapshot?.documents?.mapNotNull { doc ->
                                doc.toObject(Expense::class.java)?.copy(id = doc.id)
                            } ?: listOf()

                            Log.d("HaushaltsbuchViewModel", "Gelesene Einträge: ${expenses.size}")

                            // Gesamtliste im LiveData
                            _allExpenses.value = expenses

                            // Falls schon ein Tag ausgewählt war, neu laden
                            loadExpensesForDate(formatDate(selectedDate))
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("HaushaltsbuchViewModel", "Fehler beim Laden der WG-ID: ${e.message}")
                }
        }
    }

    /**
     * Lädt alle Einträge, die zu einem bestimmten Monat gehören,
     * wobei das Datum in "dd.MM.yyyy" gespeichert ist.
     * monthYearFormat = "MM.yyyy" (z. B. "03.2025").
     */
    fun loadExpensesForMonth(monthYearFormat: String) {
        val all = _allExpenses.value ?: emptyList()

        val filtered = all.filter { expense ->
            // Datum z. B. "15.03.2025" => split('.')
            val parts = expense.datum.split(".")
            if (parts.size == 3) {
                val month = parts[1] // "03"
                val year = parts[2]  // "2025"
                val expenseMonthYear = "$month.$year" // "03.2025"

                expenseMonthYear == monthYearFormat
            } else false
        }

        _selectedDateExpenses.value = filtered
        updateTotals()
    }

    /**
     * Lädt alle Einträge für einen bestimmten Tag (z. B. "15.03.2025").
     */
    fun loadExpensesForDate(dateString: String) {
        Log.d("HaushaltsbuchViewModel", "Lade Einträge für Tag: $dateString")
        viewModelScope.launch {
            val all = _allExpenses.value ?: emptyList()
            val filtered = all.filter { it.datum == dateString }
            _selectedDateExpenses.value = filtered

            updateTotals()
        }
    }

    /**
     * Fügt einen neuen Eintrag hinzu. Das Datum soll "dd.MM.yyyy" sein,
     * damit die Filterfunktionen korrekt funktionieren.
     */
    fun addExpenseToFirestore(expense: Expense) {
        fetchUserToken { token ->
            if (token == null) {
                Log.e("HaushaltsbuchViewModel", "Kein User-Token, Abbruch (addExpense).")
                return@fetchUserToken
            }
            val userEmail = token.email
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { doc ->
                    val wgId = doc.getString("wgId") ?: return@addOnSuccessListener
                    val userNickname = doc.getString("nickname") ?: "Unbekannt"

                    val docRef = db.collection("WGs").document(wgId)
                        .collection("Haushaltsbuch").document()

                    val expenseWithUser = expense.copy(
                        id = docRef.id,
                        userEmail = userEmail,
                        userNickname = userNickname
                    )

                    docRef.set(expenseWithUser)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Neuer Eintrag ID=${docRef.id} erfolgreich hinzugefügt.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Fehler beim Hinzufügen: ${e.message}")
                        }
                }
        }
    }

    /**
     * Aktualisiert einen vorhandenen Eintrag (Transaction) in Firestore anhand seiner ID.
     */
    fun updateExpenseInFirestore(expense: Expense) {
        Log.d("HaushaltsbuchViewModel", "Aktualisiere Eintrag mit ID=${expense.id}")
        fetchUserToken { token ->
            if (token == null) {
                Log.e("HaushaltsbuchViewModel", "Kein Token, Abbruch (updateExpense).")
                return@fetchUserToken
            }
            val userEmail = token.email
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { doc ->
                    val wgId = doc.getString("wgId")
                    if (wgId == null || expense.id.isEmpty()) {
                        Log.e("HaushaltsbuchViewModel", "WG-ID oder expense ID leer, Update abgebrochen.")
                        return@addOnSuccessListener
                    }
                    db.collection("WGs").document(wgId)
                        .collection("Haushaltsbuch")
                        .document(expense.id)
                        .set(expense)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Eintrag aktualisiert: ID=${expense.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Fehler beim Aktualisieren: ${e.message}")
                        }
                }
        }
    }

    /**
     * Löscht einen vorhandenen Eintrag aus Firestore.
     */
    fun deleteExpenseFromFirestore(expense: Expense) {
        Log.d("HaushaltsbuchViewModel", "Lösche Eintrag mit ID=${expense.id}")
        fetchUserToken { token ->
            if (token == null) {
                Log.e("HaushaltsbuchViewModel", "Kein Token, Abbruch (deleteExpense).")
                return@fetchUserToken
            }
            val userEmail = token.email
            db.collection("users").document(userEmail).get()
                .addOnSuccessListener { doc ->
                    val wgId = doc.getString("wgId")
                    if (wgId == null || expense.id.isEmpty()) {
                        Log.e("HaushaltsbuchViewModel", "WG-ID oder expense ID leer, Löschen abgebrochen.")
                        return@addOnSuccessListener
                    }
                    db.collection("WGs").document(wgId)
                        .collection("Haushaltsbuch")
                        .document(expense.id)
                        .delete()
                        .addOnSuccessListener {
                            Log.d("Firestore", "Eintrag gelöscht: ID=${expense.id}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Fehler beim Löschen: ${e.message}")
                        }
                }
        }
    }

    /**
     * Aktualisiert den Kontostand = Summe(Einnahmen) - Summe(Ausgaben)
     * basierend auf _selectedDateExpenses (also der aktuell gefilterten Liste).
     */
    private fun updateTotals() {
        val selectedList = _selectedDateExpenses.value ?: emptyList()

        val totalIncome = selectedList
            .filter { it.istEinnahme }
            .sumOf { it.betrag.toDouble() }
            .toFloat()

        val totalExpense = selectedList
            .filter { !it.istEinnahme }
            .sumOf { it.betrag.toDouble() }
            .toFloat()

        _kontostand.value = totalIncome - totalExpense

        Log.d("HaushaltsbuchViewModel",
            "Kontostand neu berechnet: Einnahmen=$totalIncome, Ausgaben=$totalExpense, Stand=${_kontostand.value}"
        )
    }

    /**
     * Konvertiert ein Calendar-Objekt in "dd.MM.yyyy" (z. B. "15.03.2025"),
     * wird häufig bei loadExpensesForDate-Aufrufen genutzt.
     */
    fun formatDate(calendar: Calendar): String {
        val df = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return df.format(calendar.time)
    }

    /**
     * Gibt eine Farbe (Int) für eine bestimmte Kategorie zurück,
     * z. B. für ein Tortendiagramm.
     */
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

    /**
     * Liest den User-Token (Kotlin Flow) aus UserPreferences aus
     * und ruft action(token) auf, sobald er vorliegt.
     */
    private fun fetchUserToken(action: (UserToken?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                action(userToken)
            }
        }
    }
}
