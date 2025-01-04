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

    /**
     * Load WG details based on the logged-in user.
     */
    fun loadWgDetails(userEmail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Fetch user document
                val userDoc = db.collection("users").document(userEmail).get().await()
                val wgId = userDoc.getString("wgId")
                if (!wgId.isNullOrEmpty()) {
                    _wgId.postValue(wgId) // Update LiveData for WG ID
                    fetchWgDetails(wgId)
                } else {
                    logError("WG ID not found for user: $userEmail")
                }
            } catch (e: Exception) {
                logError("Failed to fetch user details for email: $userEmail", e)
            }
        }
    }


    /**
     * Fetch WG details using WG ID.
     */
    private suspend fun fetchWgDetails(wgId: String) {
        try {
            val wgDoc = db.collection("WGs").document(wgId).get().await()

            if (wgDoc != null) {
                // Update LiveData with WG details
                _wgAddress.postValue(wgDoc.getString("adresse") ?: "Adresse nicht verfügbar")
                _roomCount.postValue(wgDoc.getString("zimmerAnzahl") ?: "0")
                _wgSize.postValue(wgDoc.getString("groesse") ?: "0 m²")

                // Fetch roommates (mitgliederEmails)
                val bewohnerEmails = wgDoc.get("mitgliederEmails") as? List<String> ?: emptyList()
                fetchRoommateDetails(bewohnerEmails)
            } else {
                logError("WG document is null for ID: $wgId")
            }
        } catch (e: Exception) {
            logError("Failed to fetch WG details for ID: $wgId", e)
        }
    }

    /**
     * Fetch and update the list of roommates based on email list.
     */
    private suspend fun fetchRoommateDetails(emails: List<String>) {
        val roommates = mutableListOf<Pair<String, String>>()
        for (email in emails) {
            try {
                val userDoc = db.collection("users").document(email).get().await()
                val nickname = userDoc.getString("nickname") ?: "Unbekannt"
                roommates.add(Pair(nickname, email))

                // Update LiveData once all roommates are fetched
                if (roommates.size == emails.size) {
                    _bewohnerList.postValue(roommates)
                }
            } catch (e: Exception) {
                logError("Failed to fetch roommate details for email: $email", e)
            }
        }
    }

    /**
     * Set WG details manually for editing purposes.
     */
    fun setWgDetails(address: String, rooms: String, size: String) {
        _wgAddress.value = address
        _roomCount.value = rooms
        _wgSize.value = size
    }

    /**
     * Log errors in a consistent way.
     */
    private fun logError(message: String, exception: Exception? = null) {
        if (exception != null) {
            Log.e("WgSharedViewModel", message, exception)
        } else {
            Log.e("WgSharedViewModel", message)
        }
    }
}
