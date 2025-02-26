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

    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY)


    init {
        initializeDaysOfWeek()
        loadTasks()
        checkAndCalculateMonthlyPoints()
    }


    fun initializeDaysOfWeek() {
        val calendar = Calendar.getInstance()
        // Setze den Wochenstart auf Montag
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

        val days = mutableListOf<String>()
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY)

        for (i in 0..6) { // 7 Tage hinzufügen (Montag bis Sonntag)
            days.add(dateFormat.format(calendar.time))
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Zum nächsten Tag wechseln
        }

        daysOfWeek = days
        Log.d("WochenplanViewModel", "Initialisierte Tage: $daysOfWeek")
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

                        if (wgId.isNullOrEmpty()) {
                            Log.e("WochenplanViewModel", "wgId ist null oder leer. Kann keine Daten abrufen.")
                            return@addOnSuccessListener
                        }

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
                    }
                    .addOnFailureListener { e ->
                        Log.e("WochenplanViewModel", "Error getting user data", e)
                    }
            }
        }
    }

    fun deleteFutureRepeatingTasks(task: DynamicTask) {
        Log.d("WochenplanViewModel", "🔍 Starte Kettenlöschung für: ${task.description} mit ID: ${task.id} und parentTaskId: ${task.parentTaskId}")

        fetchUserToken { token ->
            token?.let { userToken ->
                db.collection("users").document(userToken.email).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId)
                                .collection("Wochenplan")
                                .whereEqualTo("parentTaskId", task.id) // 🔥 Alle Kinder der aktuellen Aufgabe finden
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val tasksToDelete = snapshot.documents.mapNotNull { it.toObject(DynamicTask::class.java) }

                                    Log.d("WochenplanViewModel", "🔥 Gefundene zukünftige Instanzen: ${tasksToDelete.size}")

                                    for (doc in snapshot.documents) {
                                        Log.d("WochenplanViewModel", "🔥 Lösche: ${doc.getString("description")}, Datum: ${doc.getString("date")}")

                                        doc.reference.delete()
                                            .addOnSuccessListener {
                                                Log.d("WochenplanViewModel", "✅ Erfolgreich gelöscht: ${doc.getString("description")}")

                                                val deletedTask = doc.toObject(DynamicTask::class.java)
                                                if (deletedTask != null) {
                                                    deleteFutureRepeatingTasks(deletedTask) // 🔥 Löscht die nächste Stufe
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("WochenplanViewModel", "❌ Fehler beim Löschen von ${doc.getString("description")}", e)
                                            }
                                    }

                                    // ✅ **Lösche die ursprüngliche Startaufgabe zuletzt**
                                    db.collection("WGs").document(wgId)
                                        .collection("Wochenplan")
                                        .document(task.id)
                                        .delete()
                                        .addOnSuccessListener {
                                            Log.d("WochenplanViewModel", "✅ Startaufgabe erfolgreich gelöscht: ${task.description}")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("WochenplanViewModel", "❌ Fehler beim Löschen der Startaufgabe", e)
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("WochenplanViewModel", "❌ Fehler beim Abrufen zukünftiger wiederholender Aufgaben", e)
                                }
                        }
                    }
            }
        }
    }











    private fun categorizeTasksByWeek(tasksList: List<DynamicTask>) {
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)

        val updatedTasks = mutableListOf<DynamicTask>()

        for (task in tasksList) {
            val taskDate = parseDate(task.date)

            // Prüfe, ob das Datum in der Vergangenheit liegt (aber NICHT heute)
            if (taskDate.before(today) && !task.wasUpdated) {
                if (!task.isDone) {  // Nur überfällige Aufgaben werden angepasst
                    if (task.points > 1) {
                        val updatedTask = task.copy(
                            points = task.points / 2,
                            wasUpdated = true
                        )
                        updateTask(updatedTask) // Firestore aktualisieren
                        updatedTasks.add(updatedTask)
                    } else {
                        updatedTasks.add(task)
                    }
                } else {
                    updatedTasks.add(task)
                }
            } else {
                updatedTasks.add(task)
            }
        }

        _tasks.value = updatedTasks

        _lastWeekTasks.value = updatedTasks.filter { isLastWeek(parseDate(it.date)) }
            .sortedBy { parseDate(it.date) }

        _thisWeekTasks.value = updatedTasks.filter { isThisWeek(parseDate(it.date)) }
            .sortedBy { parseDate(it.date) }

        _nextWeekTasks.value = updatedTasks.filter { isNextWeek(parseDate(it.date)) }
            .sortedBy { parseDate(it.date) }
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
                                        // 🚀 Falls es eine wiederholende Aufgabe ist, sofort `processRepeatingTasks()` starten
                                        if (task.repeating) {
                                            processRepeatingTasks(listOf(task))
                                        }
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

    private val _todayUserTasks = MutableLiveData<List<DynamicTask>>()
    val todayUserTasks: LiveData<List<DynamicTask>> = _todayUserTasks


    //aufgaben für den jeweiligen nutzer für den jeweiligen Tag (für homepage fragment)

    fun loadTodayUserTasks() {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                val todayDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.GERMANY).format(Date())

                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (!wgId.isNullOrEmpty()) {
                            db.collection("WGs").document(wgId)
                                .collection("Wochenplan")
                                .whereEqualTo("date", todayDate)
                                .whereEqualTo("assigneeEmail", userEmail)
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val tasksList = snapshot.documents.mapNotNull { it.toObject(DynamicTask::class.java) }
                                    _todayUserTasks.postValue(tasksList)
                                }
                                .addOnFailureListener { e ->
                                    Log.e("WochenplanViewModel", "Fehler beim Laden der Aufgaben", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("WochenplanViewModel", "Fehler beim Abrufen der WG-ID", e)
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


    fun calculateMonthlyPoints(wgId: String) {
        db.collection("WGs")
            .document(wgId)
            .collection("Wochenplan")
            .get()
            .addOnSuccessListener { snapshot ->
                val userPoints = mutableMapOf<String, Int>()

                snapshot.documents.forEach { doc ->
                    val task = doc.toObject(DynamicTask::class.java)
                    if (task != null && isLastWeek(parseDate(task.date)) && !task.isAccounted) {
                        val points = if (task.isDone) task.points else -task.points
                        userPoints[task.assigneeEmail] = (userPoints[task.assigneeEmail] ?: 0) + points

                        // ⚠️ Setze `isAccounted = true`, damit es nicht erneut berechnet wird
                        db.collection("WGs")
                            .document(wgId)
                            .collection("Wochenplan")
                            .document(task.id)
                            .update("isAccounted", true)
                    }
                }

                saveMonthlyPoints(wgId, userPoints)
                saveLifetimePoints(wgId, userPoints)
                updateLastCalculationDate(wgId) // ✅ Speichere das Berechnungsdatum
            }
    }


    private fun saveMonthlyPoints(wgId: String, userPoints: Map<String, Int>) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val monthIdentifier = SimpleDateFormat("yyyy-MM", Locale.GERMANY).format(calendar.time)

        val monthRef = db.collection("WGs")
            .document(wgId)
            .collection("PunkteHistorie")
            .document(monthIdentifier)

        monthRef.get().addOnSuccessListener { document ->
            val existingPoints = if (document.exists()) {
                document.get("points") as? MutableMap<String, Long> ?: mutableMapOf()
            } else {
                mutableMapOf()
            }

            userPoints.forEach { (userEmail, points) ->
                existingPoints[userEmail] = (existingPoints[userEmail] ?: 0) + points
            }

            monthRef.set(mapOf("month" to monthIdentifier, "points" to existingPoints))
                .addOnSuccessListener {
                    Log.d("WochenplanViewModel", "Punkte für $monthIdentifier gespeichert.")
                }
                .addOnFailureListener { e ->
                    Log.e("WochenplanViewModel", "Fehler beim Speichern der Punkte", e)
                }
        }
    }





    fun checkAndCalculateMonthlyPoints() {
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.TUESDAY) return // Beende, falls heute nicht Dienstag ist

        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email

                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs")
                                .document(wgId)
                                .get()
                                .addOnSuccessListener { wgDoc ->
                                    val lastCalculationDate = wgDoc.getString("lastCalculationDate")
                                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())

                                    // ✅ Falls heute bereits berechnet wurde, beende die Methode
                                    if (lastCalculationDate == today) {
                                        Log.d("WochenplanViewModel", "Punkte wurden heute bereits berechnet.")
                                        return@addOnSuccessListener
                                    }

                                    // 🚀 Falls noch nicht berechnet, führe die Berechnung durch
                                    calculateMonthlyPoints(wgId)
                                }
                        }
                    }
            }
        }
    }


    private fun updateLastCalculationDate(wgId: String) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.GERMANY).format(Date())

        db.collection("WGs")
            .document(wgId)
            .update("lastCalculationDate", today)
            .addOnSuccessListener {
                Log.d("WochenplanViewModel", "Letzte Berechnung wurde auf $today gesetzt.")
            }
            .addOnFailureListener { e ->
                Log.e("WochenplanViewModel", "Fehler beim Speichern des letzten Berechnungsdatums", e)
            }
    }

    fun deductPointsBeforeUnassigning(userEmail: String, points: Int) {
        if (userEmail.isEmpty() || points <= 0) return // Kein gültiger User oder keine Punkte

        fetchUserToken { token ->
            token?.let { userToken ->
                db.collection("users").document(userToken.email).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId") ?: return@addOnSuccessListener
                        val monthIdentifier = SimpleDateFormat("yyyy-MM", Locale.GERMANY).format(Date())

                        val monthRef = db.collection("WGs")
                            .document(wgId)
                            .collection("PunkteHistorie")
                            .document(monthIdentifier)

                        monthRef.get().addOnSuccessListener { monthDoc ->
                            val existingPoints = if (monthDoc.exists()) {
                                monthDoc.get("points") as? MutableMap<String, Long> ?: mutableMapOf()
                            } else {
                                mutableMapOf()
                            }

                            // Punkte für den aktuellen Zuständigen abziehen
                            val currentPoints = existingPoints[userEmail] ?: 0
                            existingPoints[userEmail] = currentPoints - points

                            // Firestore aktualisieren
                            monthRef.set(mapOf("month" to monthIdentifier, "points" to existingPoints))
                                .addOnSuccessListener {
                                    Log.d("WochenplanViewModel", "Punkte für $userEmail abgezogen: -$points")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("WochenplanViewModel", "Fehler beim Abziehen der Punkte", e)
                                }
                        }
                    }
            }
        }
    }

    private fun saveLifetimePoints(wgId: String, userPoints: Map<String, Int>) {
        val lifetimeRef = db.collection("WGs")
            .document(wgId)
            .collection("lifetimePoints")
            .document("gesamt")

        lifetimeRef.get().addOnSuccessListener { document ->
            val existingPoints = if (document.exists()) {
                document.get("points") as? MutableMap<String, Long> ?: mutableMapOf()
            } else {
                mutableMapOf()
            }

            userPoints.forEach { (userEmail, points) ->
                existingPoints[userEmail] = (existingPoints[userEmail] ?: 0) + points
            }

            lifetimeRef.set(mapOf("points" to existingPoints))
                .addOnSuccessListener {
                    Log.d("WochenplanViewModel", "Lifetime-Punkte aktualisiert.")
                }
                .addOnFailureListener { e ->
                    Log.e("WochenplanViewModel", "Fehler beim Speichern der Lifetime-Punkte", e)
                }
        }
    }

    private fun isSameDay(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR)
    }





    private fun processRepeatingTasks(tasks: List<DynamicTask>) {
        Log.d("WochenplanViewModel", "Starte processRepeatingTasks mit ${tasks.size} Aufgaben")

        fetchUserToken { token ->
            token?.let { userToken ->
                db.collection("users").document(userToken.email).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId)
                                .collection("Wochenplan")
                                .get()
                                .addOnSuccessListener { snapshot ->
                                    val existingTasks = snapshot.documents.mapNotNull { it.toObject(DynamicTask::class.java) }
                                    val existingTaskDates = existingTasks.associateBy { it.date }
                                    val newTasks = mutableListOf<DynamicTask>()

                                    for (task in tasks) {
                                        if (task.repeating) {
                                            val taskDate = parseDate(task.date)
                                            val endDate = task.repeatUntil?.let { parseDate(it) } // Enddatum parsen
                                            var previousTaskId = task.parentTaskId ?: task.id

                                            when (task.repeatFrequency?.trim()) {
                                                "Täglich" -> {
                                                    val tempDate = taskDate.clone() as Calendar
                                                    while (endDate == null || tempDate.before(endDate) || isSameDay(tempDate, endDate)) {
                                                        val formattedDate = dateFormat.format(tempDate.time)
                                                        if (!existingTaskDates.containsKey(formattedDate)) {
                                                            val newTask = task.copy(
                                                                id = UUID.randomUUID().toString(),
                                                                date = formattedDate,
                                                                isDone = false,
                                                                parentTaskId = previousTaskId
                                                            )
                                                            newTasks.add(newTask)
                                                            previousTaskId = newTask.id
                                                        }
                                                        tempDate.add(Calendar.DAY_OF_YEAR, 1)
                                                    }
                                                }

                                                "Wöchentlich" -> {
                                                    val tempDate = taskDate.clone() as Calendar
                                                    while (endDate == null || tempDate.before(endDate) || isSameDay(tempDate, endDate)) {
                                                        val formattedDate = dateFormat.format(tempDate.time)
                                                        if (!existingTaskDates.containsKey(formattedDate)) {
                                                            val newTask = task.copy(
                                                                id = UUID.randomUUID().toString(),
                                                                date = formattedDate,
                                                                isDone = false,
                                                                parentTaskId = previousTaskId
                                                            )
                                                            newTasks.add(newTask)
                                                            previousTaskId = newTask.id
                                                        }
                                                        tempDate.add(Calendar.WEEK_OF_YEAR, 1)
                                                    }
                                                }

                                                "Monatlich" -> {
                                                    val tempDate = taskDate.clone() as Calendar
                                                    while (endDate == null || tempDate.before(endDate) || isSameDay(tempDate, endDate)) {
                                                        val formattedDate = dateFormat.format(tempDate.time)
                                                        if (!existingTaskDates.containsKey(formattedDate)) {
                                                            val newTask = task.copy(
                                                                id = UUID.randomUUID().toString(),
                                                                date = formattedDate,
                                                                isDone = false,
                                                                parentTaskId = previousTaskId
                                                            )
                                                            newTasks.add(newTask)
                                                            previousTaskId = newTask.id
                                                        }
                                                        tempDate.add(Calendar.MONTH, 1)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (newTasks.isNotEmpty()) {
                                        for (newTask in newTasks) {
                                            addTask(newTask)
                                        }
                                    } else {
                                        Log.d("WochenplanViewModel", "Keine neuen wiederholenden Aufgaben erforderlich")
                                    }
                                }
                        }
                    }
            }
        }
    }

}




