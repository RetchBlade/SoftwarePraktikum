package com.serenitysystems.livable.ui.einkaufsliste.data

import android.graphics.Color
import java.util.Date
import java.util.UUID

// Datenklasse für ein Produkt in der Einkaufsliste
data class Produkt(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var quantity: String = "",
    var unit: String = "",
    var category: String = "",
    var imageUri: String? = "",        // URI des Produktfotos
    var date: String? = "",           // Datum, an dem das Produkt benötigt wird
    var isChecked: Boolean = false,     // Gibt an, ob das Produkt abgehakt wurde
    var isPurchasedToday: Boolean = false, // Gibt an, ob das Produkt heute gekauft wurde
    var statusColor: Int = Color.WHITE, // Farbe zur Darstellung des Status
    var statusIcon: Int? = null         // Icon zur Darstellung des Status (z.B. Häkchen, Warnung)
)
