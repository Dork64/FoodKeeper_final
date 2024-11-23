package com.example.foodkeeper_final.models

data class User(
    val email: String = "",
    val profileIcon: String = "",
    val shoppingList: List<ShoppingItem> = emptyList()
)
