package com.serenitysystems.livable.ui.einkaufsliste

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EinkaufslisteViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Einkaufsliste"
    }
    val text: LiveData<String> = _text
}