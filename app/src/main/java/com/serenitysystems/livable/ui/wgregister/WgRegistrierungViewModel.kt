package com.serenitysystems.livable.ui.wgregister

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.wohngesellschaft.data.Wg

class WgRegistrierungViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Methode zum Registrieren der WG
    fun registerWg(wg: Wg, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("WGs")
            .add(wg)
            .addOnSuccessListener { documentReference ->
                // Erfolgreiche Registrierung, sende die WG-Dokument-ID zurück
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    // Methode zum Aktualisieren der Benutzerdaten in der Firestore-Datenbank
    fun updateUserInFirestore(email: String, wgId: String, wgRole: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        val userRef = db.collection("users").document(email)
        userRef.update(mapOf(
            "wgId" to wgId,
            "wgRole" to wgRole
        )).addOnSuccessListener {
            onSuccess() // Erfolgreiche Aktualisierung
        }.addOnFailureListener { exception ->
            onFailure(exception) // Fehlerfall
        }
    }
}
