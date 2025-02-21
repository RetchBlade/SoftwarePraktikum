package com.serenitysystems.livable.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import com.serenitysystems.livable.ui.wochenplan.WochenplanViewModel
import com.serenitysystems.livable.ui.wochenplan.data.DynamicTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomePageViewModel(application: Application) : AndroidViewModel(application) {

    private val _userNickname = MutableLiveData<String?>()
    val userNickname: LiveData<String?> = _userNickname
    private val _userPic = MutableLiveData<String?>()
    val userPic: LiveData<String?> = _userPic // Hier auf _userPic korrigiert
    private val firestore = FirebaseFirestore.getInstance()
    private val userPreferences: UserPreferences = UserPreferences(application)

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                if (userToken != null) {
                    firestore.collection("users")
                        .document(userToken.email)  // Verwende die E-Mail als Document ID
                        .addSnapshotListener { documentSnapshot, e ->
                            if (e != null) {
                                Log.w("com.serenitysystems.livable.ui.home.HomePageViewModel", "Fehler beim Abhören der Firestore-Updates", e)
                                return@addSnapshotListener
                            }
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                val nickname = documentSnapshot.getString("nickname") ?: ""
                                _userNickname.postValue(nickname)
                                val profileImageUrl = documentSnapshot.getString("profileImageUrl") ?: ""
                                _userPic.postValue(profileImageUrl)
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
    private val _todayTasks = MutableLiveData<List<DynamicTask>>()
    val todayTasks: LiveData<List<DynamicTask>> = _todayTasks

    fun fetchTodayTasks(wochenplanViewModel: WochenplanViewModel) {
        wochenplanViewModel.todayUserTasks.observeForever { tasks ->
            _todayTasks.postValue(tasks)
        }
        wochenplanViewModel.loadTodayUserTasks()
    }



    private var isSyncing: Boolean = false // Verhindert wiederholte Abrufe während der Synchronisation

    fun joinWG(wgId: String, onError: (String) -> Unit) {
        if (isSyncing) return

        isSyncing = true
        fetchUserToken { token ->
            token?.let { userToken ->
                val userRef = firestore.collection("users").document(userToken.email)
                userRef.get().addOnSuccessListener { wgDocument ->
                    if (wgDocument.exists()) {
                        val userEmail = userToken.email
                        val lifetimeRef = firestore.collection("WGs").document(wgId).collection("lifetimePoints").document("gesamt")

                        // Überprüfen, ob Nutzer bereits existiert
                        lifetimeRef.get().addOnSuccessListener { lifetimeDoc ->
                            val existingPoints = if (lifetimeDoc.exists()) {
                                lifetimeDoc.get("points") as? MutableMap<String, Long> ?: mutableMapOf()
                            } else {
                                mutableMapOf()
                            }

                            if (existingPoints.containsKey(userEmail)) {
                                // Nutzer existiert bereits → Normaler Beitritt
                                updateUserWG(userRef, wgId, onError)
                            } else {
                                // Nutzer existiert nicht → Durchschnitt berechnen
                                val averagePoints = if (existingPoints.isNotEmpty()) {
                                    existingPoints.values.sum() / existingPoints.size
                                } else {
                                    0 // Falls keine Mitglieder da sind
                                }

                                // Punkte dem neuen Mitglied geben
                                existingPoints[userEmail] = averagePoints

                                // Lifetime-Punkte speichern
                                lifetimeRef.set(mapOf("points" to existingPoints)).addOnSuccessListener {
                                    Log.d("HomePageViewModel", "Neuer Nutzer erhält durchschnittlich $averagePoints Punkte.")

                                    // Nutzer offiziell zur WG hinzufügen
                                    updateUserWG(userRef, wgId, onError)
                                }.addOnFailureListener { e ->
                                    Log.e("HomePageViewModel", "Fehler beim Speichern der Lifetime-Punkte", e)
                                    onError("Fehler beim Speichern der Punkte.")
                                }
                            }
                        }
                    } else {
                        onError("Die WG-ID existiert nicht.")
                        isSyncing = false
                    }
                }.addOnFailureListener { exception ->
                    Log.e("HomePageViewModel", "Fehler beim Laden der WG-Daten: ${exception.message}")
                    isSyncing = false
                }
            }
        }
    }

    private fun updateUserWG(userRef: DocumentReference, wgId: String, onError: (String) -> Unit) {
        userRef.update("wgId", wgId, "wgRole", "Wg-Mitglied")
            .addOnSuccessListener {
                Log.d("HomePageViewModel", "Erfolgreich der WG beigetreten.")
            }
            .addOnFailureListener { exception ->
                Log.e("HomePageViewModel", "Fehler beim Beitritt zur WG: ${exception.message}")
                onError("Fehler beim Beitritt zur WG.")
            }
            .addOnCompleteListener {
                isSyncing = false // Synchronisation abgeschlossen
            }
    }


    fun leaveWG() {
        if (isSyncing) return
        isSyncing = true
        fetchUserToken { token ->
            token?.let { userToken ->
                val userRef = FirebaseFirestore.getInstance().collection("users").document(userToken.email)
                userRef.update("wgId", "", "wgRole", "")
                    .addOnSuccessListener {
                        Log.d("com.serenitysystems.livable.ui.home.HomePageViewModel", "Erfolgreich aus der WG verlassen.")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("com.serenitysystems.livable.ui.home.HomePageViewModel", "Error leaving the WG: ${exception.message}")
                    }
                    .addOnCompleteListener {
                        isSyncing = false
                    }
            }
        }
    }

    fun fetchUserWGInfo(onSuccess: (String?, String?) -> Unit, onError: (String) -> Unit) {
        fetchUserToken { token ->
            token?.let { userToken ->
                if (userToken != null) {
                    firestore.collection("users")
                        .document(userToken.email)  // Verwende die E-Mail als Document ID
                        .addSnapshotListener { documentSnapshot, e ->
                            if (e != null) {
                                Log.w(
                                    "com.serenitysystems.livable.ui.home.HomePageViewModel",
                                    "Fehler beim Abhören der Firestore-Updates",
                                )
                                return@addSnapshotListener
                            }
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                val wgId = documentSnapshot.getString("wgId") ?: ""

                                val wgRole = documentSnapshot.getString("wgRole") ?: ""
                                onSuccess(wgId, wgRole)
                            } else {
                                onError("Benutzerdaten nicht gefunden.")
                                Log.w(
                                    "com.serenitysystems.livable.ui.home.HomePageViewModel",
                                    "Benutzerdaten nicht gefunden.",
                                )
                            }
                        }
                }
                return@fetchUserToken
            }
        }
    }
}
