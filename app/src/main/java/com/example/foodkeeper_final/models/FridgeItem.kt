package com.example.foodkeeper_final.models

data class FridgeItem(
    var id: String = "",
    var name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val addedDate: Long = 0, // Timestamp когда продукт был добавлен в холодильник
    var expiryDays: Int = 0,
    val defaultStorageDays: Int = 0,
){

    fun getExpiryDate(): Long = addedDate + (expiryDays * 24 * 60 * 60 * 1000L)

    fun isExpired(): Boolean = System.currentTimeMillis() > getExpiryDate()

    fun getRemainingDays(): Int {
        val remainingMillis = getExpiryDate() - System.currentTimeMillis()
        return (remainingMillis / (24 * 60 * 60 * 1000L)).toInt()
    }
}
