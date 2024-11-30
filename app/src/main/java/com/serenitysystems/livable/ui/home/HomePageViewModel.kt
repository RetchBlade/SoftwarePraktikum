package com.serenitysystems.livable.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
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

    private var isSyncing: Boolean = false // Verhindert wiederholte Abrufe während der Synchronisation

    fun joinWG(wgId: String, onError: (String) -> Unit) {
        if (isSyncing) return

        isSyncing = true
        fetchUserToken { token ->
            token?.let { userToken ->
                val userRef = FirebaseFirestore.getInstance().collection("users").document(userToken.email)
                userRef.get().addOnSuccessListener { wgDocument ->
                    if (wgDocument.exists()) {
                        userRef.update("wgId", wgId, "wgRole", "Wg-Mitglied")
                            .addOnSuccessListener {
                                Log.d("com.serenitysystems.livable.ui.home.HomePageViewModel", "Erfolgreich die Wg beigetreten.")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("com.serenitysystems.livable.ui.home.HomePageViewModel", "Fehler beim Aktualisieren des UserToken: ${exception.message}")
                            }
                            .addOnCompleteListener {
                                isSyncing = false // Synchronisation abgeschlossen
                            }
                    } else {
                        onError("Die WG-ID existiert nicht.")
                        isSyncing = false
                    }
                }.addOnFailureListener { exception ->
                    Log.e("com.serenitysystems.livable.ui.home.HomePageViewModel", "Fehler beim Laden der WG-DokumentationWG ID: ${exception.message}")
                    isSyncing = false
                }
            }
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
