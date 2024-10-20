package com.serenitysystems.livable.ui.wgregister

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.wohngesellschaft.data.Wg

class WgRegistrierungViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

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
    fun updateUserInFirestore(email: String, wgId: String, wgRole: String) {
        val userRef = db.collection("Users").document(email)
        userRef.update(mapOf(
            "wgId" to wgId.toString(),
            "wgRole" to wgRole.toString()
        )).addOnSuccessListener {
            // Erfolgreiche Aktualisierung
        }.addOnFailureListener { exception ->
            // Fehlerfall
        }
    }
}
