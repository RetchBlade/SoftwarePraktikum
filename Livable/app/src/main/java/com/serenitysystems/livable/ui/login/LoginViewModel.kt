package com.serenitysystems.livable.ui.login

import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    // Funktion zum Handhaben des Login-Prozesses
    fun login(username: String, password: String) {
        // Hier könnte der eigentliche Login-Prozess implementiert werden
    }

    // Eine einfache Passwort-Validierungsfunktion
    private fun isPasswordValid(password: String): Boolean {
        // Überprüfen, ob das Passwort länger als 5 Zeichen ist
        return password.length > 5
    }
}
