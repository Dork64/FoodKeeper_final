package com.example.foodkeeper_final.models

import java.util.Calendar

data class FridgeItem(
    var id: String = "",
    var name: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val addedDate: Long = 0, // Timestamp когда продукт был добавлен в холодильник
    var expiryDays: Int = 0,
    val defaultStorageDays: Int = 0,
){

    fun getExpiryDate(): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = addedDate
        calendar.add(Calendar.DAY_OF_YEAR, expiryDays)
        return calendar.timeInMillis
    }

    fun isExpired(): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        // Сбрасываем время до начала дня
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val expiryCalendar = Calendar.getInstance()
        expiryCalendar.timeInMillis = getExpiryDate()
        // Сбрасываем время до начала дня
        expiryCalendar.set(Calendar.HOUR_OF_DAY, 0)
        expiryCalendar.set(Calendar.MINUTE, 0)
        expiryCalendar.set(Calendar.SECOND, 0)
        expiryCalendar.set(Calendar.MILLISECOND, 0)
        
        return calendar.after(expiryCalendar)
    }

    fun getRemainingDays(): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        // Сбрасываем время до начала дня
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val expiryCalendar = Calendar.getInstance()
        expiryCalendar.timeInMillis = getExpiryDate()
        // Сбрасываем время до начала дня
        expiryCalendar.set(Calendar.HOUR_OF_DAY, 0)
        expiryCalendar.set(Calendar.MINUTE, 0)
        expiryCalendar.set(Calendar.SECOND, 0)
        expiryCalendar.set(Calendar.MILLISECOND, 0)
        
        val diffInMillis = expiryCalendar.timeInMillis - calendar.timeInMillis
        return (diffInMillis / (24 * 60 * 60 * 1000)).toInt()
    }
}
