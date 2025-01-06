package com.serenitysystems.livable.ui.einkaufsliste

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.serenitysystems.livable.ui.einkaufsliste.data.Produkt
import com.serenitysystems.livable.ui.login.data.UserPreferences
import com.serenitysystems.livable.ui.login.data.UserToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EinkaufslisteViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val userPreferences: UserPreferences = UserPreferences(application)
    private val _itemsByDate = MutableLiveData<MutableMap<String, MutableList<Produkt>>>()
    val itemsByDate: LiveData<MutableMap<String, MutableList<Produkt>>> = _itemsByDate

    init {
        loadItems()
    }

    // Lädt alle Items für das aktuelle Benutzer-WG
    private fun loadItems() {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId).collection("Einkaufsliste")
                                .addSnapshotListener { snapshot, e ->
                                    if (e != null) return@addSnapshotListener
                                    val currentMap = mutableMapOf<String, MutableList<Produkt>>()
                                    snapshot?.documents?.forEach { doc ->
                                        val item = doc.toObject(Produkt::class.java)
                                        item?.let {
                                            val dateKey = it.date.toString()
                                            currentMap.getOrPut(dateKey) { mutableListOf() }.add(it)
                                        }
                                    }
                                    _itemsByDate.postValue(currentMap)
                                }
                        }
                    }
            }
        }
    }

    // Fügt ein Item hinzu
    fun addItem(date: String, item: Produkt) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId).collection("Einkaufsliste")
                                .document(item.id)
                                .set(item)
                        }
                    }
            }
        }
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
        updateItem(newDate, item)
    }


    // Aktualisiert ein Item
    fun updateItem(date: String, item: Produkt) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            db.collection("WGs").document(wgId).collection("Einkaufsliste")
                                .document(item.id)
                                .set(item)
                        }
                    }
            }
        }
    }

    // Aktualisiere das Bild eines Produkts
    fun updateItemImage(date: String, item: Produkt) {
        val currentMap = _itemsByDate.value ?: mutableMapOf()
        val itemsForDate = currentMap[date.toString()]?.toMutableList() ?: mutableListOf()
        val index = itemsForDate.indexOfFirst { it.id == item.id }
        if (index != -1) {
            itemsForDate[index] = item
            currentMap[date.toString()] = itemsForDate

            // LiveData aktualisieren
            _itemsByDate.postValue(currentMap.toMutableMap())
        }
    }

    fun uploadImageToFirebaseStorage(uri: Uri, itemId: String, onUploadComplete: (String?) -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("Einkaufsliste/$itemId.jpg")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                // Abrufen der Download-URL nach erfolgreichem Hochladen
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d("FirebaseStorage", "Download-URL: $downloadUri")
                    onUploadComplete(downloadUri.toString()) // Korrekte URL zurückgeben
                }
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseStorage", "Fehler beim Hochladen: ${e.message}")
                onUploadComplete(null) // Fehler behandeln
            }
    }



    // Löscht ein Item
    fun deleteItem(date: String, item: Produkt) {
        fetchUserToken { token ->
            token?.let { userToken ->
                val userEmail = userToken.email
                db.collection("users").document(userEmail).get()
                    .addOnSuccessListener { document ->
                        val wgId = document.getString("wgId")
                        if (wgId != null) {
                            // Löschen des Produkts aus Firestore
                            val itemRef = db.collection("WGs").document(wgId).collection("Einkaufsliste").document(item.id)
                            itemRef.delete().addOnSuccessListener {
                                // Wenn das Produkt gelöscht wurde, prüfe auf ein Bild
                                if (!item.imageUri.isNullOrEmpty()) {
                                    val imageRef = FirebaseStorage.getInstance().getReferenceFromUrl(item.imageUri!!)
                                    imageRef.delete().addOnSuccessListener {
                                        // Bild erfolgreich gelöscht
                                        Log.d("deleteItem", "Bild erfolgreich gelöscht: ${item.imageUri}")
                                    }.addOnFailureListener { e ->
                                        // Fehler beim Löschen des Bildes
                                        Log.e("deleteItem", "Fehler beim Löschen des Bildes: ${e.message}")
                                    }
                                }
                            }.addOnFailureListener { e ->
                                Log.e("deleteItem", "Fehler beim Löschen des Produkts: ${e.message}")
                            }
                        }
                    }
            }
        }
    }


    // Gibt alle Items für ein bestimmtes Datum zurück
    fun getItemsForDate(dateKey: String): List<Produkt> {
        return _itemsByDate.value?.get(dateKey) ?: emptyList()
    }

    private fun fetchUserToken(action: (UserToken?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.userToken.collect { userToken ->
                action(userToken)
            }
        }
    }
}
