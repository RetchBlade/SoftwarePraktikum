package com.serenitysystems.livable.ui.register.data

data class User(
    val email: String = "",
    val nickname: String = "",
    val password: String = "", // Hier speichern wir das gehashte Passwort bald
    val birthdate: String = "",
    val gender: String = ""
)
