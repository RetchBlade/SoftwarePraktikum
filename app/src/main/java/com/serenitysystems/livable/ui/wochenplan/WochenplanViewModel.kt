package com.serenitysystems.livable.ui.wochenplan

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import com.serenitysystems.livable.ui.wochenplan.data.DynamicTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WochenplanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val _tasks = MutableLiveData<List<DynamicTask>>()
    val tasks: LiveData<List<DynamicTask>> = _tasks

    private val _lastWeekTasks = MutableLiveData<List<DynamicTask>>()
    val lastWeekTasks: MutableLiveData<List<DynamicTask>> = _lastWeekTasks

    private val _thisWeekTasks = MutableLiveData<List<DynamicTask>>()
    val thisWeekTasks: MutableLiveData<List<DynamicTask>> = _thisWeekTasks

    private val _nextWeekTasks = MutableLiveData<List<DynamicTask>>()
    val nextWeekTasks: MutableLiveData<List<DynamicTask>> = _nextWeekTasks

    private val _todayTasks = MutableLiveData<List<DynamicTask>>()

    private var taskListener: ListenerRegistration? = null

    var daysOfWeek: List<String> = listOf() // List of all days displayed by ViewPager

    private val userPreferences: UserPreferences = UserPreferences(application)

    private val _assignees = MutableLiveData<List<Pair<String, String>>>()
    val assignees: LiveData<List<Pair<String, String>>> = _assignees

    init {
        loadTasks()
    }

    // Lädt die Tasks für die aktuelle WG und die entsprechenden Wochen
    private fun loadTasks() {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email

                // Hole die wgId aus der Sammlung "users" basierend auf der E-Mail
                db.collection("users")
                    .document(userEmail)
                    .get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            // Lösche alte Tasks, bevor die aktuellen Tasks geladen werden
                            deleteOldTasks(wgId)

                            // Hole die Tasks für die WG anhand der wgId
                            taskListener = db.collection("WGs")
                                .document(wgId)
                                .collection("Wochenplan")
                                .addSnapshotListener { snapshot, e ->
                                    if (e != null) {
                                        Log.w("WochenplanViewModel", "Listen failed.", e)
                                        return@addSnapshotListener
                                    }

                                    val tasksList = mutableListOf<DynamicTask>()
                                    snapshot?.documents?.forEach { document ->
                                        val task = document.toObject(DynamicTask::class.java)
                                        if (task != null) {
                                            tasksList.add(task)
                                        }
                                    }

                                    // Aufteilen der Aufgaben in verschiedene Wochen
                                    categorizeTasksByWeek(tasksList)
                                }
                        } else {
                            Log.e("WochenplanViewModel", "wgId not found for user: $userEmail")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("WochenplanViewModel", "Error getting user data", e)
                    }
            }
        }
    }


    // Kategorisiert die Aufgaben in verschiedene Wochen
    private fun categorizeTasksByWeek(tasksList: List<DynamicTask>) {
        _tasks.value = tasksList

        val lastWeek = tasksList.filter { isLastWeek(parseDate(it.date)) }
            .sortedBy { parseDate(it.date) }  // SORTIERUNG HINZUGEFÜGT ✅

        val thisWeek = tasksList.filter { isThisWeek(parseDate(it.date)) }
            .sortedBy { parseDate(it.date) }  // SORTIERUNG HINZUGEFÜGT ✅

        val nextWeek = tasksList.filter { isNextWeek(parseDate(it.date)) }
            .sortedBy { parseDate(it.date) }  // SORTIERUNG HINZUGEFÜGT ✅

        _lastWeekTasks.value = lastWeek
        _thisWeekTasks.value = thisWeek
        _nextWeekTasks.value = nextWeek
    }

    private fun parseDate(dateStr: String): Calendar {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY)
        val date = dateFormat.parse(dateStr) ?: Date()

        return Calendar.getInstance().apply {
            time = date
        }
    }



    private fun isThisWeek(date: Calendar): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))

        // Setzt den Wochenanfang auf Montag
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfWeek = calendar.timeInMillis

        // Gehe zum Sonntag der aktuellen Woche
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfWeek = calendar.timeInMillis

        return date.timeInMillis in startOfWeek..endOfWeek
    }

    fun isLastWeek(date: Calendar): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))

        // Eine Woche zurücksetzen
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfLastWeek = calendar.timeInMillis

        // Gehe zum Sonntag der letzten Woche
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfLastWeek = calendar.timeInMillis

        return date.timeInMillis in startOfLastWeek..endOfLastWeek
    }

    private fun isNextWeek(date: Calendar): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))

        // Eine Woche vorwärts setzen
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfNextWeek = calendar.timeInMillis

        // Gehe zum Sonntag der nächsten Woche
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfNextWeek = calendar.timeInMillis

        return date.timeInMillis in startOfNextWeek..endOfNextWeek
    }


    // Überprüft, ob das Datum heute ist
    private fun isToday(date: Calendar): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
        val startOfToday = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        val endOfToday = calendar.apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis
        return date.timeInMillis in startOfToday..endOfToday
    }


    // Fügt eine neue Aufgabe hinzu
    fun addTask(task: DynamicTask) {
        fetchUserToken { token ->
            token?.let { userToken ->
                if (userToken != null) {
                    val userEmail = userToken.email

                    // Hole die wgId aus der Sammlung "users" basierend auf der E-Mail
                    db.collection("users")
                        .document(userEmail)
                        .get()
                        .addOnSuccessListener { document ->
                            val wgId = document.getString("wgId")
                            if (wgId != null) {
                                // Füge die Aufgabe zur richtigen WG hinzu
                                db.collection("WGs")
                                    .document(wgId)
                                    .collection("Wochenplan")
                                    .document(task.id)
                                    .set(task)
                                    .addOnSuccessListener {
                                        Log.d("WochenplanViewModel", "Task added successfully.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("WochenplanViewModel", "Error adding task", e)
                                    }
                            } else {
                                Log.e("WochenplanViewModel", "wgId not found for user: $userEmail")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("WochenplanViewModel", "Error getting user data", e)
                        }
                }
            }
        }
    }

    // Aktualisiert eine bestehende Aufgabe
    fun updateTask(updatedTask: DynamicTask) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users")
                    .document(userEmail)
                    .get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            // Aufgabe in Firestore aktualisieren
                            db.collection("WGs")
                                .document(wgId)
                                .collection("Wochenplan")
                                .document(updatedTask.id)
                                .set(updatedTask)
                                .addOnSuccessListener {
                                    Log.d("WochenplanViewModel", "Task updated successfully.")

                                    // Nach dem Update die Daten neu laden, um die UI zu aktualisieren
                                    loadTasks()  // Hier wird die Methode aufgerufen, die den SnapshotListener aktualisiert
                                }
                                .addOnFailureListener { e ->
                                    Log.e("WochenplanViewModel", "Error updating task", e)
                                }
                        } else {
                            Log.e("WochenplanViewModel", "wgId not found for user: $userEmail")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("WochenplanViewModel", "Error getting user data", e)
                    }
            }
        }
    }


    // Löscht eine Aufgabe
    fun deleteTask(task: DynamicTask) {
        fetchUserToken { token ->
            token?.let { userToken ->
                if (userToken != null) {
                    val userEmail = userToken.email
                    db.collection("users")
                        .document(userEmail)
                        .get()
                        .addOnSuccessListener { document ->
                            val wgId = document.getString("wgId")
                            if (wgId != null) {
                                db.collection("WGs")
                                    .document(wgId)
                                    .collection("Wochenplan")
                                    .document(task.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        Log.d("WochenplanViewModel", "Task deleted successfully.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("WochenplanViewModel", "Error deleting task", e)
                                    }
                            } else {
                                Log.e("WochenplanViewModel", "wgId not found for user: $userEmail")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("WochenplanViewModel", "Error getting user data", e)
                        }
                }
            }
        }
    }
    private fun deleteOldTasks(wgId: String) {
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY)
        val calendar = Calendar.getInstance()

        // Setze das Datum auf 15 Tage vor heute
        calendar.add(Calendar.DAY_OF_YEAR, -15)
        val fifteenDaysAgo = calendar.time

        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email

                // Hole die wgId aus der Sammlung "users" basierend auf der E-Mail
                db.collection("users")
                    .document(userEmail)
                    .get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            // Greife auf die Tasksammlung der WG zu
                            db.collection("WGs")
                                .document(wgId)
                                .collection("Wochenplan")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    snapshot?.documents?.forEach { document ->
                                        val taskDateStr = document.getString("date")
                                        val taskDate = try {
                                            dateFormat.parse(taskDateStr!!)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            null
                                        }

                                        // Prüfe, ob das Datum älter als 15 Tage ist
                                        if (taskDate != null && taskDate.before(fifteenDaysAgo)) {
                                            // Lösche den Task aus der Datenbank
                                            db.collection("WGs")
                                                .document(wgId)
                                                .collection("Wochenplan")
                                                .document(document.id)
                                                .delete()
                                                .addOnSuccessListener {
                                                    Log.d("Wochenplan", "Task gelöscht: ${document.id}")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Wochenplan", "Fehler beim Löschen des Tasks", e)
                                                }
                                        }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Wochenplan", "Fehler beim Abrufen der Tasks", e)
                                }
                        } else {
                            Log.e("Wochenplan", "wgId nicht gefunden für den Benutzer: $userEmail")
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Wochenplan", "Fehler beim Abrufen der Benutzerdaten", e)
                    }
            }
        }
    }


    // Vergiss nicht, den Listener zu entfernen, wenn der ViewModel nicht mehr benötigt wird
    override fun onCleared() {
        super.onCleared()
        taskListener?.remove()
    }

    fun fetchUserToken(action: (UserToken?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                action(userToken)
            }
        }
    }

    fun loadAssignees(onAssigneesLoaded: (List<Pair<String, String>>) -> Unit = {}) {
        if (!_assignees.value.isNullOrEmpty()) {
            onAssigneesLoaded(_assignees.value!!)
            return
        }



        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users")
                    .document(userEmail)
                    .get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs")
                                .document(wgId)
                                .get()
                                .addOnSuccessListener { wgDocument ->
                                    val memberEmails =
                                        wgDocument.get("mitgliederEmails") as? List<String>
                                            ?: emptyList()
                                    val userNames = mutableListOf<Pair<String, String>>()

                                    memberEmails.forEach { email ->
                                        db.collection("users")
                                            .document(email)
                                            .get()
                                            .addOnSuccessListener { userDocument ->
                                                val nickname = userDocument.getString("nickname")
                                                if (nickname != null) {
                                                    userNames.add(Pair(nickname, email))
                                                }
                                                if (userNames.size == memberEmails.size) {
                                                    _assignees.postValue(userNames)
                                                    onAssigneesLoaded(userNames)
                                                }
                                            }
                                    }
                                }
                        }
                    }
            }
        }
    }

    fun claimTask(task: DynamicTask, userEmail: String, userName: String) {
        fetchUserToken { token ->
            token?.let {
                db.collection("WGs")
                    .document(it.email) // Get wgId here
                    .collection("Wochenplan")
                    .document(task.id)
                    .update(
                        "assignee", userName,
                        "assigneeEmail", userEmail
                    )
                    .addOnSuccessListener {
                        Log.d("WochenplanViewModel", "Task claimed successfully.")
                        loadTasks() // Refresh data
                    }
                    .addOnFailureListener { e ->
                        Log.e("WochenplanViewModel", "Failed to claim task", e)
                    }
            }
        }
    }

}




