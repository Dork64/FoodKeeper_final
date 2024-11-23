package com.example.foodkeeper_final.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodkeeper_final.R
import com.example.foodkeeper_final.models.FridgeItem

class FridgeAdapter(
    private val items: MutableList<FridgeItem>,
    private val onEdit: (FridgeItem) -> Unit, // Callback для редактирования
    private val onDelete: (FridgeItem, Int) -> Unit // Callback для удаления
) : RecyclerView.Adapter<FridgeAdapter.FridgeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FridgeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fridge, parent, false)
        return FridgeViewHolder(view, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: FridgeViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int = items.size

    class FridgeViewHolder(
        itemView: View,
        private val onEdit: (FridgeItem) -> Unit,
        private val onDelete: (FridgeItem, Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val productName: TextView = itemView.findViewById(R.id.productName)
        private val productImage: ImageView = itemView.findViewById(R.id.productImage)
        private val ivEdit: ImageView = itemView.findViewById(R.id.ivEditItem) // Кнопка "Редактировать"
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDeleteItem) // Кнопка "Удалить"

        fun bind(item: FridgeItem, position: Int) {
            productName.text = item.name

            // Загрузка изображения с помощью Glide
            if (item.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_error)
                    .into(productImage)
            } else {
                productImage.setImageResource(R.drawable.ic_placeholder)
            }

            // Обработчики кнопок
            ivEdit.setOnClickListener { onEdit(item) }
            ivDelete.setOnClickListener { onDelete(item, position) }
        }
    }
}
