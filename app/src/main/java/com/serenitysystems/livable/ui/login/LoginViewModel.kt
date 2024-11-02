package com.serenitysystems.livable.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    val loginSuccess = MutableLiveData<Boolean>()
    val loginError = MutableLiveData<String>()
    private val userPreferences = UserPreferences(application)

    suspend fun loginUser(email: String, password: String) {
        try {
            // Überprüfe, ob der Benutzer existiert
            val existingUser = db.collection("users")
                .document(email)
                .get()
                .await()

            if (existingUser.exists()) {
                // Hole das Benutzerobjekt
                val user = existingUser.toObject(UserToken::class.java)
                user?.let {
                    // Vergleiche das gehashte Passwort
                    val hashedPassword = hashPassword(password)
                    if (it.password == hashedPassword) {
                        // Speichere UserToken in DataStore
                        viewModelScope.launch {
                            userPreferences.saveUserToken(it)
                        }
                        loginSuccess.postValue(true)
                    } else {
                        loginError.postValue("Falsches Passwort")
                    }
                } ?: run {
                    loginError.postValue("Ein unbekannter Fehler ist aufgetreten")
                }
            } else {
                loginError.postValue("Benutzer existiert nicht")
            }
        } catch (e: Exception) {
            loginError.postValue("Ein unbekannter Fehler ist aufgetreten: ${e.message}")
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
}
