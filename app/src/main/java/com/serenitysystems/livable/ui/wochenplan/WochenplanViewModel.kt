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
                if (userToken != null) {
                    val userEmail = userToken.email

                    // Hole die wgId aus der Sammlung "users" basierend auf der E-Mail
                    db.collection("users")
                        .document(userEmail)
                        .get()
                        .addOnSuccessListener { document ->
                            val wgId = document.getString("wgId")
                            if (wgId != null) {
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
    }

    // Kategorisiert die Aufgaben in verschiedene Wochen
    private fun categorizeTasksByWeek(tasksList: List<DynamicTask>) {
        _tasks.value = tasksList
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))


        val lastWeek = mutableListOf<DynamicTask>()
        val thisWeek = mutableListOf<DynamicTask>()
        val nextWeek = mutableListOf<DynamicTask>()
        val today = mutableListOf<DynamicTask>()

        tasksList.forEach { task ->
            val taskDate = try {
                calendar.apply { time = dateFormat.parse(task.date) ?: Date() }
            } catch (e: Exception) {
                Log.e("WochenplanViewModel", "Error parsing task date: ${task.date}", e)
                calendar // Fallback auf das aktuelle Datum, falls Parsing fehlschlägt
            }

            // Vergleiche das Datum und kategorisiere die Aufgabe
            when {
                isThisWeek(taskDate) -> thisWeek.add(task)
                isLastWeek(taskDate) -> lastWeek.add(task)
                isNextWeek(taskDate) -> nextWeek.add(task)
                isToday(taskDate) -> today.add(task)
            }
        }

        // Setze die LiveData-Werte für jede Woche
        _thisWeekTasks.value = thisWeek
        _lastWeekTasks.value = lastWeek
        _nextWeekTasks.value = nextWeek
        _todayTasks.value = today

        Log.d("WochenplanViewModel", "This week tasks: ${thisWeek.size}")
        Log.d("WochenplanViewModel", "Last week tasks: ${lastWeek.size}")
        Log.d("WochenplanViewModel", "Next week tasks: ${nextWeek.size}")
        Log.d("WochenplanViewModel", "Today's tasks: ${today.size}")
    }

    private fun isThisWeek(date: Calendar): Boolean {
        // Kalender in der Berlin-Zeitzone erstellen
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))

        // Setze den Start der Woche auf Montag
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.clear(Calendar.MINUTE)
        calendar.clear(Calendar.SECOND)

        // Speichere den Start der Woche (Montag)
        val startOfWeek = calendar.timeInMillis

        // Setze den Endpunkt auf Sonntag der gleichen Woche
        calendar.add(Calendar.DAY_OF_WEEK, 6) // Gehe auf Sonntag
        val endOfWeek = calendar.timeInMillis

        // Prüfe, ob das aktuelle Datum innerhalb des Wochenbereichs liegt
        Log.d("WochenplanViewModel", "Current Date: ${date.timeInMillis}")
        Log.d("WochenplanViewModel", "Start of Week (Monday): $startOfWeek")
        Log.d("WochenplanViewModel", "End of Week (Sunday): $endOfWeek")

        return date.timeInMillis in (startOfWeek - 1000)..(endOfWeek + 1000)
    }


    // Überprüft, ob das Datum in der letzten Woche ist
    private fun isLastWeek(date: Calendar): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfLastWeek = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endOfLastWeek = calendar.timeInMillis
        return date.timeInMillis in startOfLastWeek..endOfLastWeek
    }

    // Überprüft, ob das Datum in der nächsten Woche ist
    private fun isNextWeek(date: Calendar): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"))
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfNextWeek = calendar.timeInMillis
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

    // Vergiss nicht, den Listener zu entfernen, wenn der ViewModel nicht mehr benötigt wird
    override fun onCleared() {
        super.onCleared()
        taskListener?.remove()
    }

    private fun fetchUserToken(action: (UserToken?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                action(userToken)
            }
        }
    }

    fun loadAssignees(onAssigneesLoaded: (List<Pair<String, String>>) -> Unit) {
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

                                    // Hole die Benutzernamen basierend auf den Mitglieds-E-Mails
                                    val userNames =
                                        mutableListOf<Pair<String, String>>() // Nickname, Email
                                    memberEmails.forEach { email ->
                                        db.collection("users")
                                            .document(email)
                                            .get()
                                            .addOnSuccessListener { userDocument ->
                                                val nickname = userDocument.getString("nickname")
                                                if (nickname != null) {
                                                    userNames.add(
                                                        Pair(
                                                            nickname,
                                                            email
                                                        )
                                                    ) // Nickname und E-Mail speichern
                                                }

                                                // Wenn alle Benutzernamen abgerufen sind, rufe den Callback auf
                                                if (userNames.size == memberEmails.size) {
                                                    _assignees.postValue(userNames) // Assignees setzen
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
}



