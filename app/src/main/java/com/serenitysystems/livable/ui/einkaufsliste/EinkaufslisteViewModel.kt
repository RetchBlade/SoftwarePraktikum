package com.serenitysystems.livable.ui.einkaufsliste

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class EinkaufslisteViewModel : ViewModel() {

    private val _produkte = MutableLiveData<List<Produkt>>()
    val produkte: LiveData<List<Produkt>> = _produkte

    init {
        _produkte.value = emptyList()
    }

    // Fügt ein neues Produkt zur Liste hinzu
    fun addProdukt(produkt: Produkt) {
        val currentList = _produkte.value?.toMutableList() ?: mutableListOf()
        currentList.add(produkt)
        _produkte.value = currentList
    }

    // Aktualisiert ein bestehendes Produkt
    fun updateProdukt(updatedProdukt: Produkt) {
        val currentList = _produkte.value?.toMutableList() ?: mutableListOf()
        val index = currentList.indexOfFirst { it.name == updatedProdukt.name }
        if (index != -1) {
            currentList[index] = updatedProdukt
            _produkte.value = currentList
        }
    }
}
