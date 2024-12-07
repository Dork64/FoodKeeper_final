package com.example.foodkeeper_final.models

data class ShoppingItem(
    var id: String = "",
    var name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val defaultStorageDays: Int = 0,  // стандартный срок хранения в днях
    var quantity: String = "",
    var unit: String = ""
)
