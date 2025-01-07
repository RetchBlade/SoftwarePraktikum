package com.serenitysystems.livable.ui.userprofil

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.serenitysystems.livable.ui.login.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfilansichtViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val userPreferences = UserPreferences(application)

    private val _profileImage = MutableLiveData<String?>()
    val profileImage: LiveData<String?> = _profileImage

    private val _username = MutableLiveData<String?>()
    val username: LiveData<String?> = _username

    private val _email = MutableLiveData<String?>()
    val email: LiveData<String?> = _email

    private val _birthdate = MutableLiveData<String?>()
    val birthdate: LiveData<String?> = _birthdate

    private val _gender = MutableLiveData<String?>()
    val gender: LiveData<String?> = _gender

    private val _role = MutableLiveData<String?>()
    val role: LiveData<String?> = _role

    private var snapshotListener: ListenerRegistration? = null


    fun loadCurrentUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                userToken?.let {
                    // Setze einen Snapshot-Listener für Echtzeit-Updates
                    snapshotListener?.remove() // Entferne alte Listener, falls vorhanden
                    snapshotListener = firestore.collection("users").document(it.email)
                        .addSnapshotListener { snapshot, e ->
                            if (e != null) {
                                Log.e("ProfilansichtViewModel", "SnapshotListener Fehler: ${e.message}")
                                return@addSnapshotListener
                            }
                            if (snapshot != null && snapshot.exists()) {
                                _profileImage.postValue(snapshot.getString("profileImageUrl"))
                                _username.postValue(snapshot.getString("nickname"))
                                _email.postValue(snapshot.getString("email"))
                                _birthdate.postValue(snapshot.getString("birthdate"))
                                _gender.postValue(snapshot.getString("gender"))
                                _role.postValue(snapshot.getString("wgRole"))
                            } else {
                                Log.w("ProfilansichtViewModel", "Snapshot ist null oder existiert nicht")
                            }
                        }
                }
            }
        }
    }


    fun loadUserData(email: String) {
        firestore.collection("users").document(email)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ProfilansichtViewModel", "Fehler beim Laden der Benutzerdaten: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _profileImage.postValue(snapshot.getString("profileImageUrl"))
                    _username.postValue(snapshot.getString("nickname"))
                    _email.postValue(snapshot.getString("email"))
                    _birthdate.postValue(snapshot.getString("birthdate"))
                    _gender.postValue(snapshot.getString("gender"))
                    _role.postValue(snapshot.getString("wgRole"))
                } else {
                    Log.w("ProfilansichtViewModel", "Keine Benutzerdaten gefunden.")
                }
            }
    }


    override fun onCleared() {
        super.onCleared()
        // Entferne den Snapshot-Listener beim Beenden
        snapshotListener?.remove()
    }
}
