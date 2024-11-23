package com.example.foodkeeper_final.fragments

import FridgeAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodkeeper_final.R

class FridgeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Привязываем layout для фрагмента
        val view = inflater.inflate(R.layout.fragment_fridge, container, false)

        // Инициализируем RecyclerView
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewFridge)

        // Установим менеджер для отображения сеткой
        recyclerView.layoutManager = GridLayoutManager(context, 3) // 3 элемента в строке

        // Установим адаптер с тестовыми данными
        val items = listOf(
            "Молоко", "Хлеб", "Яйца", "Сыр"
        ) // Пример данных
        recyclerView.adapter = FridgeAdapter(items)

        return view
    }
}
