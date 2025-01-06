package com.serenitysystems.livable.ui.profilverwalten

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfilverwaltenViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val userPreferences = UserPreferences(application)

    private val _liveUserData = MutableLiveData<UserToken?>()
    val liveUserData: LiveData<UserToken?> get() = _liveUserData

    private var userListener: ListenerRegistration? = null
    private var currentEmail: String? = null

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { token ->
                token?.email?.let { email ->
                    currentEmail = email
                    observeUserData(email)
                }
            }
        }
    }

    private fun observeUserData(email: String) {
        // Entferne vorherigen Listener, falls vorhanden
        userListener?.remove()

        // Setze einen Echtzeit-Listener für die Benutzerdaten
        userListener = firestore.collection("users").document(email)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _liveUserData.postValue(null)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val userData = snapshot.toObject(UserToken::class.java)
                    _liveUserData.postValue(userData)
                } else {
                    _liveUserData.postValue(null)
                }
            }
    }

    fun updateUserData(updatedUser: UserToken, newImageUri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val email = currentEmail ?: return@launch

                // Aktualisiere das Bild, falls ein neues hochgeladen wird
                newImageUri?.let {
                    val imageRef = FirebaseStorage.getInstance().reference.child("profile_images/$email.jpg")
                    imageRef.putFile(it).await()
                    val downloadUrl = imageRef.downloadUrl.await()
                    updatedUser.profileImageUrl = downloadUrl.toString()
                }

                // Speichere die aktualisierten Benutzerdaten in Firestore
                firestore.collection("users").document(email).set(updatedUser).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove() // Entferne den Listener, um Speicherlecks zu vermeiden
    }
}
