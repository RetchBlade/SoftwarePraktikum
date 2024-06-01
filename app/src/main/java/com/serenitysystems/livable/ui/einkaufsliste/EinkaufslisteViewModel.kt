package com.serenitysystems.livable.ui.einkaufsliste

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EinkaufslisteViewModel : ViewModel() {

    // Private MutableLiveData, die den Text speichert und initialisiert
    private val _text = MutableLiveData<String>().apply {
        value = "Einkaufsliste" // Initialer Wert für die LiveData
    }

    // Öffentlich zugängliche LiveData, die von der UI beobachtet werden kann
    val text: LiveData<String> = _text
}
