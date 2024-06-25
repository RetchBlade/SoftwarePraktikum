package com.serenitysystems.livable.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.ui.login.data.User

class LoginViewModel : ViewModel() {
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    private val _loginResult = MutableLiveData<Boolean>()
    val loginResult: LiveData<Boolean> get() = _loginResult

    fun login(email: String, password: String) {
        // Implementiere hier deine Login-Logik
        // Der Einfachheit halber nehmen wir an, dass der Login immer erfolgreich ist
        _user.value = User(email, password)
        _loginResult.value = true
    }
}