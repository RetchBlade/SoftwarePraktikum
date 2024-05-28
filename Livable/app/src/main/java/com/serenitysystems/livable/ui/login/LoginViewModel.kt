package com.serenitysystems.livable.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns

import com.serenitysystems.livable.R

class LoginViewModel() : ViewModel() {

    fun login(username: String, password: String) {

    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}