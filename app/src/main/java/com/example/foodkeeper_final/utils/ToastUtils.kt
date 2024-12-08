package com.example.foodkeeper_final.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.foodkeeper_final.R

// Утилитный класс для Toast
object ToastUtils {

    fun showCustomToast(message: String, context: Context) {
        // Создаём стандартный Toast
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)

        // Получаем view Toast
        val toastView = toast.view

        // Устанавливаем фон Toast
        val background = GradientDrawable()
        background.cornerRadius = 32f
        background.setColor(ContextCompat.getColor(context, R.color.background_color))
        background.setStroke(2, ContextCompat.getColor(context, R.color.list_item_text))
        toastView?.background = background

        // Получаем TextView внутри Toast и меняем его стиль
        val toastText = toastView?.findViewById<TextView>(android.R.id.message)
        toastText?.setTextColor(context.getColor(R.color.list_item_text))
        toastText?.textSize = 14f

        val textBackground = GradientDrawable()
        textBackground.setColor(ContextCompat.getColor(context, R.color.background_color))
        toastText?.background = textBackground

        toast.show()
    }
}
