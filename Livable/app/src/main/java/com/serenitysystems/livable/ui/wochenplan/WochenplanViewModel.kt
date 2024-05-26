package com.serenitysystems.livable.ui.wochenplan

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WochenplanViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Wochenplan kommt noch!"
    }
    val text: LiveData<String> = _text
}