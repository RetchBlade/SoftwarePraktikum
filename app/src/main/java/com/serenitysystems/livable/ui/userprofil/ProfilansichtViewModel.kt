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

    private val _lifetimePoints = MutableLiveData<Int>()
    val lifetimePoints: LiveData<Int> = _lifetimePoints

    private val _rankImageUrl = MutableLiveData<String?>()
    val rankImageUrl: LiveData<String?> = _rankImageUrl


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

    fun loadLifetimePoints(email: String) {
        firestore.collection("users").document(email).get()
            .addOnSuccessListener { userDoc ->
                val wgId = userDoc.getString("wgId")
                if (!wgId.isNullOrEmpty()) {
                    firestore.collection("WGs")
                        .document(wgId)
                        .collection("lifetimePoints")
                        .document("gesamt")
                        .get()
                        .addOnSuccessListener { lifetimeDoc ->
                            val pointsData = lifetimeDoc.get("points") as? Map<String, Long>
                            val userPoints = pointsData?.get(email)?.toInt() ?: 0
                            _lifetimePoints.postValue(userPoints)
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfilansichtViewModel", "Fehler beim Laden der Lifetime-Punkte: ${e.message}")
                            _lifetimePoints.postValue(0)
                        }
                }
            }
    }

    private fun getRank(points: Int): String {
        return when {
            points < 500 -> "neuling"
            points < 1000 -> "bronze"
            points < 3000 -> "silber"
            points < 5000 -> "gold"
            else -> "champion"
        }
    }

    fun loadRankImage(points: Int) {
        val rank = getRank(points)
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("ranks").document("ranks").get()
            .addOnSuccessListener { document ->
                val imageUrl = document.getString(rank)
                if (!imageUrl.isNullOrEmpty()) {
                    _rankImageUrl.postValue(imageUrl)
                } else {
                    Log.e("ProfilansichtViewModel", "Kein Bild für Rang $rank gefunden")
                }
            }
            .addOnFailureListener {
                Log.e("ProfilansichtViewModel", "Fehler beim Abrufen des Rank-Bildes: ${it.message}")
            }
    }


    override fun onCleared() {
        super.onCleared()
        // Entferne den Snapshot-Listener beim Beenden
        snapshotListener?.remove()
    }
}
