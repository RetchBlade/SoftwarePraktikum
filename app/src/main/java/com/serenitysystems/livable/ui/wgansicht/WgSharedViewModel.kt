package com.serenitysystems.livable.ui.wgansicht

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.login.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WgSharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _wgAddress = MutableLiveData<String>()
    val wgAddress: LiveData<String> get() = _wgAddress

    private val _roomCount = MutableLiveData<String>()
    val roomCount: LiveData<String> get() = _roomCount

    private val _wgSize = MutableLiveData<String>()
    val wgSize: LiveData<String> get() = _wgSize

    private val _bewohnerList = MutableLiveData<List<Pair<String, String>>>()
    val bewohnerList: LiveData<List<Pair<String, String>>> get() = _bewohnerList

    private val _wgId = MutableLiveData<String?>()
    val wgId: MutableLiveData<String?> get() = _wgId

    private val _currentUserEmail = MutableLiveData<String?>()
    val currentUserEmail: LiveData<String?> get() = _currentUserEmail

    private val firestore = FirebaseFirestore.getInstance()
    private val userPreferences: UserPreferences = UserPreferences(application)

    init {
        loadUserEmailAndWgDetails()
    }

    fun loadUserEmailAndWgDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                userToken?.email?.let { email ->
                    _currentUserEmail.postValue(email)
                    firestore.collection("users").document(email).get().addOnSuccessListener { document ->
                        val wgId = document.getString("wgId") ?: ""
                        _wgId.postValue(wgId)
                        CoroutineScope(Dispatchers.IO).launch {
                            fetchWgDetails(wgId)
                        }
                    }.addOnFailureListener { e ->
                        logError("Fehler beim Abrufen der Nutzer-Daten", e)
                    }
                }
            }
        }
    }

    private fun fetchWgDetails(wgId: String) {
        firestore.collection("WGs").document(wgId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    logError("Fehler beim Abrufen der Echtzeit-Daten", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _wgAddress.postValue(snapshot.getString("adresse") ?: "Adresse nicht verfügbar")
                    _roomCount.postValue(snapshot.getString("zimmerAnzahl") ?: "0")
                    _wgSize.postValue(snapshot.getString("groesse") ?: "0")

                    val bewohnerEmails = snapshot.get("mitgliederEmails") as? List<String> ?: emptyList()
                    viewModelScope.launch(Dispatchers.IO) {
                        fetchRoommateDetails(bewohnerEmails)
                    }
                }
            }
    }




    private suspend fun fetchRoommateDetails(emails: List<String>) {
        val roommates = mutableListOf<Pair<String, String>>()
        emails.forEach { email ->
            try {
                val userDoc = firestore.collection("users").document(email).get().await()
                val nickname = userDoc.getString("nickname") ?: "Unbekannt"
                roommates.add(Pair(nickname, email))
            } catch (e: Exception) {
                logError("Fehler beim Abrufen der Bewohner-Daten für $email: ${e.localizedMessage}", e)
            }
        }
        if (roommates.isNotEmpty()) {
            _bewohnerList.postValue(roommates)
        }
    }


    fun setWgDetails(address: String, rooms: String, size: String) {
        _wgAddress.value = address
        _roomCount.value = rooms
        _wgSize.value = size
    }

    private fun logError(message: String, exception: Exception? = null) {
        Log.e("WgSharedViewModel", message, exception)
    }
}
