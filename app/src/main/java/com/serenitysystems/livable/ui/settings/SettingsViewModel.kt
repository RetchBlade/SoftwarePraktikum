package com.serenitysystems.livable.ui.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val userPreferences = UserPreferences(application)

    private val _userPic = MutableLiveData<String?>()
    val userPic: LiveData<String?> = _userPic // Hier auf _userPic korrigiert

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                if (userToken != null) {
                    db.collection("users")
                        .document(userToken.email)  // Verwende die E-Mail als Document ID
                        .addSnapshotListener { documentSnapshot, e ->
                            if (e != null) {
                                Log.w("SettingsViewModel", "Fehler beim Abhören der Firestore-Updates", e)
                                return@addSnapshotListener
                            }
                            if (documentSnapshot != null && documentSnapshot.exists()) {
                                val profileImageUrl = documentSnapshot.getString("profileImageUrl") ?: ""
                                _userPic.postValue(profileImageUrl)
                            }
                        }
                }
            }
        }
    }
    fun fetchUserToken(action: (UserToken?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { token ->
                action(token)
            }
        }
    }


}
