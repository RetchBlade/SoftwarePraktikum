// Produkt.kt

package com.serenitysystems.livable.ui.einkaufsliste

import android.graphics.Color

// Datenklasse für ein Produkt in der Einkaufsliste
data class Produkt(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String,
    var imageUri: String? = null,        // URI des Produktfotos
    var date: String? = null,            // Datum, an dem das Produkt benötigt wird
    var isChecked: Boolean = false,      // Gibt an, ob das Produkt abgehakt wurde
    var isPurchasedToday: Boolean = false, // Gibt an, ob das Produkt heute gekauft wurde
    var statusColor: Int = Color.WHITE,  // Farbe zur Darstellung des Status
    var statusIcon: Int? = null          // Icon zur Darstellung des Status (z.B. Häkchen, Warnung)
)
