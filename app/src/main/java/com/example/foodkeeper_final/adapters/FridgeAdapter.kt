package com.example.foodkeeper_final.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodkeeper_final.R
import com.example.foodkeeper_final.models.FridgeItem

class FridgeAdapter(
    private val items: List<FridgeItem>,
    private val onEdit: (FridgeItem) -> Unit,
    private val onDelete: (FridgeItem) -> Unit,
    private val onItemClick: (FridgeItem) -> Unit
) : RecyclerView.Adapter<FridgeAdapter.FridgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FridgeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_fridge, parent, false)
        return FridgeViewHolder(view)
    }

    override fun onBindViewHolder(holder: FridgeViewHolder, position: Int) {
        val product = items[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = items.size

    inner class FridgeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImage: ImageView = itemView.findViewById(R.id.productImage)
        private val productName: TextView = itemView.findViewById(R.id.productName)
        private val expiryInfo: TextView = itemView.findViewById(R.id.expiryInfo)
        private val quantityInfo: TextView = itemView.findViewById(R.id.quantityInfo)

        fun bind(product: FridgeItem) {
            // Отображаем название продукта
            productName.text = product.name

            // Отображаем количество и единицу измерения
            val quantityText = if (product.quantity.isNotEmpty() && product.unit.isNotEmpty()) {
                "${product.quantity} ${product.unit}"
            } else {
                "1 шт"
            }
            quantityInfo.text = quantityText

            // Отображаем информацию о сроке годности
            val remainingDays = product.getRemainingDays()
            val expiryText = when {
                product.isExpired() -> "Срок годности истек!"
                remainingDays == 0 -> "Истекает сегодня!"
                remainingDays == 1 -> "Истекает завтра!"
                else -> "Осталось дней: $remainingDays"
            }

            expiryInfo.text = expiryText

            // Устанавливаем цвет текста в зависимости от оставшегося срока
            expiryInfo.setTextColor(when {
                product.isExpired() -> Color.RED
                remainingDays == 2 -> Color.parseColor("#FFA500") // Оранжевый
                remainingDays <= 1 -> Color.RED
                else -> Color.parseColor("#4CAF50") // Зеленый
            })

            // Загружаем изображение с помощью Glide
            Glide.with(itemView.context)
                .load(product.imageUrl) // URL изображения
                .placeholder(R.drawable.ic_placeholder) // Заглушка
                .into(productImage)

            // Установить обработчики кнопок редактирования и удаления
            itemView.setOnClickListener {
                onEdit(product) // Сработает при клике на элемент
            }

            // Обработчик для долгого нажатия на элемент
            itemView.setOnLongClickListener {
                onDelete(product) // Сработает при долгом нажатии
                true
            }

            // Устанавливаем обработчик для клика по изображению продукта
            productImage.setOnClickListener {
                onItemClick(product) // Сработает при клике на изображение
            }
        }
    }
}