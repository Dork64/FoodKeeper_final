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
import com.example.foodkeeper_final.models.ShoppingItem

class ShoppingListAdapter(
    private val items: List<ShoppingItem>,
    private val onEdit: (ShoppingItem) -> Unit, // Callback для редактирования
    private val onDelete: (ShoppingItem, Int) -> Unit, // Callback для удаления
    private val onMove: (ShoppingItem, Int) -> Unit, // Callback для перемещения с позицией
) : RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ViewHolder(view, onEdit, onDelete, onMove)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        itemView: View,
        private val onEdit: (ShoppingItem) -> Unit,
        private val onDelete: (ShoppingItem, Int) -> Unit,
        private val onMove: (ShoppingItem, Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivItemIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val ivEdit: ImageView = itemView.findViewById(R.id.ivEditItem)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDeleteItem)
        private val ivMove: ImageView = itemView.findViewById(R.id.ivMoveToFridge)

        fun bind(item: ShoppingItem) {
            tvName.text = item.name

            // Отображаем количество и единицу измерения
            val quantityText = if (item.quantity.isNotEmpty() && item.unit.isNotEmpty()) {
                "${item.quantity} ${item.unit}"
            } else {
                "Количество не указано"
            }
            tvQuantity.text = quantityText

            if (item.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.ic_placeholder)
                    .into(ivIcon)
            } else {
                ivIcon.setImageResource(R.drawable.ic_placeholder)
            }

            // Настраиваем обработчики кликов
            ivEdit.setOnClickListener { onEdit(item) }
            ivDelete.setOnClickListener { onDelete(item, adapterPosition) }
            ivMove.setOnClickListener { onMove(item, adapterPosition) }

            itemView.setOnClickListener {
                onEdit(item) // Сработает при клике на элемент
            }
        }
    }
}