package com.serenitysystems.livable.ui.wgansicht

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WgSharedViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

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

    fun loadWgDetails(userEmail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userDoc = db.collection("users").document(userEmail).get().await()
                val wgId = userDoc.getString("wgId") ?: throw Exception("WG ID nicht verfügbar")
                _wgId.postValue(wgId)
                fetchWgDetails(wgId)
            } catch (e: Exception) {
                logError("Fehler beim Laden der WG-Daten", e)
            }
        }
    }

    private suspend fun fetchWgDetails(wgId: String) {
        try {
            val wgDoc = db.collection("WGs").document(wgId).get().await()

            _wgAddress.postValue(wgDoc.getString("adresse") ?: "Adresse nicht verfügbar")
            _roomCount.postValue(wgDoc.getString("zimmerAnzahl") ?: "0")
            _wgSize.postValue(wgDoc.getString("groesse") ?: "0")

            val bewohnerEmails = wgDoc.get("mitgliederEmails") as? List<String> ?: emptyList()
            fetchRoommateDetails(bewohnerEmails)
        } catch (e: Exception) {
            logError("Fehler beim Abrufen der WG-Daten", e)
        }
    }

    private suspend fun fetchRoommateDetails(emails: List<String>) {
        val roommates = mutableListOf<Pair<String, String>>()
        emails.forEach { email ->
            try {
                val userDoc = db.collection("users").document(email).get().await()
                val nickname = userDoc.getString("nickname") ?: "Unbekannt"
                roommates.add(Pair(nickname, email))
            } catch (e: Exception) {
                logError("Fehler beim Abrufen der Bewohner-Daten für $email", e)
            }
        }
        _bewohnerList.postValue(roommates)
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
