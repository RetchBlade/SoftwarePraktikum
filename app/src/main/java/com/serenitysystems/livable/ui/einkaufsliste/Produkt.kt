package com.serenitysystems.livable.ui.einkaufsliste

import android.graphics.Color

data class Produkt(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String,
    val imageResId: Int, // Bildreferenz
    var date: String? = null, // Ausgewähltes Datum
    var isChecked: Boolean = false, // Artikel abgehakt oder nicht
    var isPurchasedToday: Boolean = false, // Wird heute gekauft oder nicht
    var statusColor: Int = Color.WHITE, // Statusfarbe des Artikels
    var statusIcon: Int? = null // Symbol für den Status des Artikels (Häkchen oder Ausrufezeichen)
)
