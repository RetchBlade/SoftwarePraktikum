package com.serenitysystems.livable.ui.profilverwalten

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfilverwaltenViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val userPreferences = UserPreferences(application)

    private val _userData = MutableLiveData<UserToken?>()
    val userData: MutableLiveData<UserToken?> get() = _userData

    private val _profileImageUrl = MutableLiveData<String>()
    val profileImageUrl: LiveData<String> get() = _profileImageUrl

    private var currentEmail: String? = null

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { token ->
                token?.email?.let { email ->
                    currentEmail = email
                    val document = firestore.collection("users").document(email).get().await()
                    val userToken = document.toObject(UserToken::class.java)
                    _userData.postValue(userToken)
                    _profileImageUrl.postValue(userToken?.profileImageUrl ?: "")
                }
            }
        }
    }

    fun updateUserData(updatedUser: UserToken, newImageUri: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val email = currentEmail ?: return@launch
                // Update image in Firebase Storage if newImageUri is provided
                newImageUri?.let {
                    val imageRef = storage.reference.child("profile_images/$email.jpg")
                    imageRef.putFile(it).await()
                    val downloadUrl = imageRef.downloadUrl.await()
                    updatedUser.profileImageUrl = downloadUrl.toString()
                }

                // Update Firestore document
                firestore.collection("users").document(email).set(updatedUser).await()
                _userData.postValue(updatedUser)
                _profileImageUrl.postValue(updatedUser.profileImageUrl)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
