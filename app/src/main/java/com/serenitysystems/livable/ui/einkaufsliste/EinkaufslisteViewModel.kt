package com.serenitysystems.livable.ui.einkaufsliste

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt

class EinkaufslisteViewModel : ViewModel() {

    // Map zum Speichern der Produkte nach Datum
    private val _itemsByDate = MutableLiveData<MutableMap<String, MutableList<Produkt>>>()
    val itemsByDate: LiveData<MutableMap<String, MutableList<Produkt>>> = _itemsByDate

    init {
        _itemsByDate.value = mutableMapOf()
    }

    // Produkt hinzufügen
    fun addItem(date: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()
        val itemsForDate = currentMap[date] ?: mutableListOf()
        itemsForDate.add(0, item)
        currentMap[date] = itemsForDate
        _itemsByDate.value = currentMap
    }

    // Produkt zu einem neuen Datum verschieben
    fun moveItemToNewDate(oldDate: String, newDate: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()

        // Entferne das Produkt vom alten Datum
        currentMap[oldDate]?.remove(item)
        if (currentMap[oldDate]?.isEmpty() == true) {
            currentMap.remove(oldDate)
        }

        // Füge das Produkt dem neuen Datum hinzu
        val itemsForNewDate = currentMap[newDate] ?: mutableListOf()
        itemsForNewDate.add(item)
        currentMap[newDate] = itemsForNewDate

        _itemsByDate.value = currentMap
    }

    // Produkt aktualisieren (Bearbeiten)
    fun updateItem(date: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()
        val itemsForDate = currentMap[date] ?: mutableListOf()
        val index = itemsForDate.indexOfFirst { it.name == item.name }
        if (index != -1) {
            itemsForDate[index] = item
            currentMap[date] = itemsForDate
            _itemsByDate.value = currentMap
        }
    }

    // Aktualisiere den Status eines Produkts
    fun updateItemStatus(date: String, item: Produkt, isPurchased: Boolean) {
        item.isChecked = isPurchased
        _itemsByDate.value = _itemsByDate.value // Trigger für LiveData-Update
    }

    // Aktualisiere das Bild eines Produkts
    fun updateItemImage(date: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()
        val itemsForDate = currentMap[date] ?: mutableListOf()
        val index = itemsForDate.indexOfFirst { it == item }
        if (index != -1) {
            itemsForDate[index] = item
            currentMap[date] = itemsForDate
            _itemsByDate.value = currentMap
        }
    }

    // Produkt löschen
    fun deleteItem(date: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()
        val itemsForDate = currentMap[date] ?: mutableListOf()
        itemsForDate.remove(item)
        currentMap[date] = itemsForDate
        _itemsByDate.value = currentMap
    }

    // Hole die Produkte für ein bestimmtes Datum
    fun getItemsForDate(date: String): List<Produkt> {
        return _itemsByDate.value?.get(date) ?: emptyList()
    }
}
