package com.serenitysystems.livable.ui.einkaufsliste

data class Produkt(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String,
    val imageResId: Int, // Bildreferenz
    val date: String? = null // Ausgewähltes Datum
) {
    var isChecked: Boolean = false
}
