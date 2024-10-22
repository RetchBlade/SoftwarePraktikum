package com.serenitysystems.livable.ui.register
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.ui.register.data.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RegistrationViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    val registrationSuccess = MutableLiveData<Boolean>()
    val registrationError = MutableLiveData<String>()
    suspend fun registerUser(user: User) {
        try {
            // Überprüfe, ob die E-Mail-Adresse bereits existiert
            val existingUser = db.collection("users")
                .document(user.email)
                .get()
                .await()
            if (existingUser.exists()) {
                registrationError.postValue("Diese Email ist bereits vergeben")
            } else {
                // Führe die Registrierung durch
                db.collection("users")
                    .document(user.email)
                    .set(user)
                    .await()
                registrationSuccess.postValue(true)
            }
        } catch (e: Exception) {
            registrationError.postValue("Ein unbekannter Fehler ist aufgetreten: ${e.message}")
        }
    }
}