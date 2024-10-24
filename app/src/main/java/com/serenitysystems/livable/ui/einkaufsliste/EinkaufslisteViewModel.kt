package com.serenitysystems.livable.ui.einkaufsliste

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt

class EinkaufslisteViewModel : ViewModel() {

    // Map zum Speichern der Produkte nach Datum und Kategorie
    private val _itemsByDate = MutableLiveData<MutableMap<String, MutableList<Produkt>>>()
    val itemsByDate: LiveData<MutableMap<String, MutableList<Produkt>>> = _itemsByDate

    init {
        _itemsByDate.value = mutableMapOf()
    }

    // Produkt hinzufügen
    fun addItem(date: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()
        val itemsForDate = currentMap[date]?.toMutableList() ?: mutableListOf()
        itemsForDate.add(0, item)
        currentMap[date] = itemsForDate
        _itemsByDate.value = currentMap.toMutableMap() // Trigger LiveData update
    }

    // Produkt zu einem neuen Datum verschieben
    fun moveItemToNewDate(oldDate: String, newDate: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()

        // Entferne das Produkt vom alten Datum
        val oldItems = currentMap[oldDate]?.toMutableList() ?: mutableListOf()
        oldItems.removeIf { it.id == item.id }
        if (oldItems.isEmpty()) {
            currentMap.remove(oldDate)
        } else {
            currentMap[oldDate] = oldItems
        }

        // Füge das Produkt dem neuen Datum hinzu
        val newItems = currentMap[newDate]?.toMutableList() ?: mutableListOf()
        newItems.add(item)
        currentMap[newDate] = newItems

        _itemsByDate.value = currentMap.toMutableMap() // Trigger LiveData update
    }

    // Produkt aktualisieren (mit Kategorie/Container)
    fun updateItem(oldDate: String, newItem: Produkt, newDate: String) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()

        // Entferne altes Item vom alten Datum
        val itemsForOldDate = currentMap[oldDate]?.toMutableList() ?: mutableListOf()
        itemsForOldDate.removeIf { it.id == newItem.id }
        if (itemsForOldDate.isEmpty()) {
            currentMap.remove(oldDate)
        } else {
            currentMap[oldDate] = itemsForOldDate
        }

        // Füge das neue Item dem neuen Datum hinzu
        val itemsForNewDate = currentMap[newDate]?.toMutableList() ?: mutableListOf()
        itemsForNewDate.add(newItem)
        currentMap[newDate] = itemsForNewDate

        // Trigger LiveData update
        _itemsByDate.value = currentMap.toMutableMap()
    }

    // Aktualisiere das Bild eines Produkts
    fun updateItemImage(date: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()
        val itemsForDate = currentMap[date]?.toMutableList() ?: mutableListOf()
        val index = itemsForDate.indexOfFirst { it.id == item.id }
        if (index != -1) {
            itemsForDate[index] = item
            currentMap[date] = itemsForDate

            // LiveData aktualisieren
            _itemsByDate.postValue(currentMap.toMutableMap())
        }
    }

    // Produkt löschen
    fun deleteItem(date: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()
        val itemsForDate = currentMap[date]?.toMutableList() ?: mutableListOf()
        itemsForDate.removeIf { it.id == item.id }
        if (itemsForDate.isEmpty()) {
            currentMap.remove(date)
        } else {
            currentMap[date] = itemsForDate
        }
        _itemsByDate.value = currentMap.toMutableMap() // Neue Map zuweisen
    }

    // Produkte für ein Datum abrufen
    fun getItemsForDate(date: String): List<Produkt> {
        return _itemsByDate.value?.get(date) ?: emptyList()
    }
}
