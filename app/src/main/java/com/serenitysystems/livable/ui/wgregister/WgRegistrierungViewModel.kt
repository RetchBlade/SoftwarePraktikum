package com.serenitysystems.livable.ui.wgregister

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.serenitysystems.livable.ui.wgregister.data.Wg

class WgRegistrierungViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    // Methode zum Registrieren der WG
    fun registerWg(
        wg: Wg, userEmail: String, onSuccess: (String) -> Unit, onFailure: (Exception) -> Unit
    ) {
        // Array mit dem ersten Benutzer (aktueller Benutzer)
        val mitgliederEmails = mutableListOf(userEmail)

        // Kopie der WG mit dem initialisierten Array
        val wgMitMitgliedern = wg.copy(mitgliederEmails = mitgliederEmails)

        db.collection("WGs")
            .add(wgMitMitgliedern)
            .addOnSuccessListener { documentReference ->
                onSuccess(documentReference.id)
            }
            .addOnFailureListener { exception ->
                onFailure(exception)
            }
    }

    fun updateUserInFirestore(
        email: String, wgId: String, wgRole: String,
        onSuccess: () -> Unit, onFailure: (Exception) -> Unit
    ) {
        val userRef = db.collection("users").document(email)
        userRef.update(mapOf(
            "wgId" to wgId,
            "wgRole" to wgRole
        )).addOnSuccessListener {
            onSuccess()
        }.addOnFailureListener { exception ->
            onFailure(exception)
        }
    }
}
