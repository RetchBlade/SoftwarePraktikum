package com.serenitysystems.livable.ui.einkaufsliste

data class Produkt(
    val name: String,
    val quantity: String,
    val unit: String,
    val category: String,
    val imageResId: Int,
    var isChecked: Boolean = false
)
