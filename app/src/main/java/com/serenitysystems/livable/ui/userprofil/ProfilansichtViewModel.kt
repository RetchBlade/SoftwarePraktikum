package com.serenitysystems.livable.ui.userprofil

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
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

    init {
        fetchUserData()
    }

    private fun fetchUserData() {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                userToken?.let {
                    firestore.collection("users").document(it.email)
                        .get()
                        .addOnSuccessListener { document ->
                            _profileImage.postValue(document.getString("profileImageUrl"))
                            _username.postValue(document.getString("nickname"))
                            _email.postValue(document.getString("email"))
                            _birthdate.postValue(document.getString("birthdate"))
                            _gender.postValue(document.getString("gender"))
                            _role.postValue(document.getString("wgRole"))
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfilansichtViewModel", "Error fetching data: ${e.message}")
                        }
                }
            }
        }
    }
}
