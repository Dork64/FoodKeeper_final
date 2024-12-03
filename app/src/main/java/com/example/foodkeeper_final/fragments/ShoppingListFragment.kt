package com.example.foodkeeper_final.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodkeeper_final.R
import com.example.foodkeeper_final.adapters.ShoppingListAdapter
import com.example.foodkeeper_final.models.FridgeItem
import com.example.foodkeeper_final.models.ShoppingItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

class ShoppingListFragment<T> : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var adapter: ShoppingListAdapter
    private val shoppingList = mutableListOf<ShoppingItem>()
    private val recentProductsList = mutableListOf<String>()
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private val suggestionsList = mutableListOf<String>()
    private val originalShoppingList = mutableListOf<ShoppingItem>() // Исходный список
    private var currentCategory: String = "Все" // По умолчанию категория "Все"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_shopping_list, container, false)

        // Инициализация RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewShoppingList)
        fabAddItem = view.findViewById(R.id.fabAddItem)
        adapter = ShoppingListAdapter(
            shoppingList,
            onEdit = { item -> showEditItemDialog(item) }, // Редактирование
            onDelete = { item, position -> deleteShoppingItem(item, position) }, // Удаление
            onMove = { item, position -> moveToFridge(item, position) } // Перемещение в холодильник
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                // Перемещение не обрабатывается
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = shoppingList[position]

                    when (direction) {
                        ItemTouchHelper.RIGHT -> moveToFridge(item, position) // Перемещение в холодильник
                        ItemTouchHelper.LEFT -> deleteShoppingItem(item, position) // Удаление элемента
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val icon: Drawable?
                val background: ColorDrawable

                if (dX > 0) { // Свайп вправо
                    background = ColorDrawable(Color.GREEN)
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_move)
                } else { // Свайп влево
                    background = ColorDrawable(Color.RED)
                    icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
                }

                val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + icon.intrinsicHeight

                if (dX > 0) { // Отрисовка для свайпа вправо
                    background.setBounds(itemView.left, itemView.top, itemView.left + dX.toInt(), itemView.bottom)
                    icon.setBounds(itemView.left + iconMargin, iconTop, itemView.left + iconMargin + icon.intrinsicWidth, iconBottom)
                } else { // Отрисовка для свайпа влево
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    icon.setBounds(itemView.right - iconMargin - icon.intrinsicWidth, iconTop, itemView.right - iconMargin, iconBottom)
                }

                background.draw(c)
                icon.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)


        val spinnerCategory: Spinner = view.findViewById(R.id.spinnerCategory)

        // Настройка Spinner
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentCategory = parent.getItemAtPosition(position).toString()
                filterShoppingList(currentCategory) // Применяем фильтр
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Инициализация Firestore
        firestore = FirebaseFirestore.getInstance()

        // Инициализация RealtimeDatabase
        databaseReference = getUserDatabaseReference()

        // Загрузка списка покупок текущего пользователя
        loadUserShoppingList()
        loadRecentProducts()

        // Кнопка "+" для добавления продукта
        fabAddItem.setOnClickListener {
            showAddItemDialog()
        }

        return view
    }

    // Получаем ссылку на базу данных текущего пользователя
    private fun getUserDatabaseReference(): DatabaseReference {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            throw IllegalStateException("Пользователь не авторизован")
        }
        val currentUserUid = currentUser.uid
        return FirebaseDatabase.getInstance().getReference("Users")
            .child(currentUserUid)
            .child("shoppingList")
    }

    // Загрузка списка покупок из Firebase Realtime Database
    private fun loadUserShoppingList() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shoppingList.clear()
                originalShoppingList.clear()

                if (snapshot.exists()) {
                    for (itemSnapshot in snapshot.children) {
                        val shoppingItem = itemSnapshot.getValue(ShoppingItem::class.java)
                        if (shoppingItem != null) {
                            shoppingList.add(shoppingItem)
                            originalShoppingList.add(shoppingItem)
                        }
                    }
                }

                if (shoppingList.isEmpty()) {
                    showEmptyListMessage()
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Ошибка загрузки списка: ${error.message}")
            }
        })
    }

    private fun updateShoppingList() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shoppingList.clear()
                originalShoppingList.clear()

                if (snapshot.exists()) {
                    for (itemSnapshot in snapshot.children) {
                        val shoppingItem = itemSnapshot.getValue(ShoppingItem::class.java)
                        if (shoppingItem != null) {
                            shoppingList.add(shoppingItem)
                            originalShoppingList.add(shoppingItem)
                        }
                    }
                }

                if (shoppingList.isEmpty()) {
                    showEmptyListMessage()
                }
                filterShoppingList(currentCategory)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Ошибка загрузки списка: ${error.message}")
            }
        })
    }

    private fun filterShoppingList(category: String) {
        shoppingList.clear()
        if (category == "Все") {
            shoppingList.addAll(originalShoppingList) // Возвращаем все элементы
        } else {
            shoppingList.addAll(originalShoppingList.filter { it.category == category }) // Фильтруем по категории
        }
        adapter.notifyDataSetChanged() // Обновляем адаптер
    }

    // Метод для отображения сообщения, что список пуст
    private fun showEmptyListMessage() {
        if (isAdded) {  // Проверка, прикреплен ли фрагмент к активности
            Toast.makeText(requireContext(), "Ваш список пуст. Добавьте товары!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAddItemDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_item, null)

        val editTextSearch = dialogView.findViewById<EditText>(R.id.etSearchProduct)
        val listViewSuggestions = dialogView.findViewById<ListView>(R.id.lvSuggestions)

        // Адаптер для отображения подсказок
        val suggestionsAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, suggestionsList)
        listViewSuggestions.adapter = suggestionsAdapter

        // Сразу заполняем список последними продуктами

        suggestionsList.clear()
        suggestionsList.addAll(recentProductsList)
        filterShoppingList(currentCategory)
        suggestionsAdapter.notifyDataSetChanged()

        // Создаем и отображаем диалог
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Добавить продукт")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .create()

        // Обработка поиска при изменении текста
        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().toLowerCase()

                if (query.isNotEmpty()) {
                    // Поиск продуктов по введенному тексту
                    searchProducts(query) { suggestions ->
                        suggestionsList.clear()
                        suggestionsList.addAll(suggestions.map { it.name.toLowerCase() })
                        suggestionsAdapter.notifyDataSetChanged()
                    }
                } else {
                    // Возврат к отображению последних использованных продуктов
                    suggestionsList.clear()
                    suggestionsList.addAll(recentProductsList)
                    suggestionsAdapter.notifyDataSetChanged()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Обработка выбора продукта из списка
        listViewSuggestions.setOnItemClickListener { _, _, position, _ ->
            val selectedProductName = suggestionsList[position]

            // Ищем продукт по имени
            loadProductFromFirestore(selectedProductName) { selectedProduct ->
                if (selectedProduct != null) {
                    // Продукт найден, добавляем его в список покупок
                    addShoppingItem(selectedProduct)

                    // Перемещаем продукт в начало списка последних продуктов
                    recentProductsList.remove(selectedProductName)
                    recentProductsList.add(0, selectedProductName)
                    if (recentProductsList.size > 4) {
                        recentProductsList.removeAt(4)
                    }
                    suggestionsAdapter.notifyDataSetChanged()
                    saveRecentProducts()

                    Toast.makeText(
                        requireContext(),
                        "Продукт добавлен: $selectedProductName",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Продукт не найден
                    Toast.makeText(requireContext(), "Продукт не найден", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // Измененная версия loadProductFromFirestore, которая возвращает объект через callback
    private fun loadProductFromFirestore(productName: String, callback: (ShoppingItem?) -> Unit) {
        val lowerCaseProductName = productName.lowercase()

        firestore.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                // Ищем среди всех документов
                for (document in documents) {
                    val product = document.toObject(ShoppingItem::class.java)
                    val productNameInDoc = product.name?.lowercase()  // Предполагаем, что у объекта ShoppingItem есть поле name

                    if (productNameInDoc == lowerCaseProductName) {
                        callback(product)  // Если нашли продукт с нужным названием, возвращаем его
                        return@addOnSuccessListener
                    }
                }
                callback(null)  // Если не нашли, возвращаем null
            }
            .addOnFailureListener {
                Log.e("FirestoreError", "Ошибка при поиске продукта по имени $productName: ${it.message}")
                callback(null)  // В случае ошибки тоже возвращаем null
            }
    }




    private fun saveRecentProducts() {
        val recentProductsRef = databaseReference.parent?.child("recentProducts")
        recentProductsRef?.setValue(recentProductsList)
            ?.addOnSuccessListener {
                Log.d("Firebase", "Список последних продуктов успешно сохранён.")
            }
            ?.addOnFailureListener { exception ->
                Log.e("FirebaseError", "Ошибка сохранения списка последних продуктов: ${exception.message}")
            }
    }

    private fun loadRecentProducts() {
        val recentProductsRef = databaseReference.parent?.child("recentProducts")
        recentProductsRef?.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recentProductsList.clear()
                snapshot.children.mapNotNullTo(recentProductsList) { it.getValue(String::class.java) }

                Log.d("Firebase", "Список последних продуктов загружен: $recentProductsList")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Ошибка загрузки списка последних продуктов: ${error.message}")
            }
        })
    }

    // Поиск продуктов в Firestore
    private fun searchProducts(query: String, callback: (List<ShoppingItem>) -> Unit) {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                val suggestions = mutableListOf<ShoppingItem>()

                for (document in documents) {
                    val product = document.toObject(ShoppingItem::class.java)
                    if (product != null && product.name.toLowerCase().startsWith(query)) { // Точное совпадение первой буквы
                        suggestions.add(product)
                    }
                }
                callback(suggestions)
            }
            .addOnFailureListener {
                Log.e("FirestoreError", "Ошибка при поиске продуктов: ${it.message}")
            }
    }

    // Добавление товара в список покупок
    private fun addShoppingItem(item: ShoppingItem) {
        val itemRef = databaseReference.push() // Генерация уникального ключа
        item.id = itemRef.key ?: return // Устанавливаем ID товара

        itemRef.setValue(item)
            .addOnSuccessListener {
                updateShoppingList()
                Log.d("Firebase", "Продукт добавлен: ${item.name}")
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Ошибка добавления: ${exception.message}")
                Toast.makeText(requireContext(), "Не удалось добавить продукт", Toast.LENGTH_SHORT).show()
            }
    }

    // Удаление элемента из списка м
    private fun deleteShoppingItem(item: ShoppingItem, position: Int) {
        // Проверка, чтобы избежать выхода за пределы массива
        if (position >= shoppingList.size || position < 0) {
            Log.e("DeleteError", "Неверный индекс: $position")
            return
        }

        // Создаем диалоговое окно для подтверждения удаления
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить товар?")
            .setMessage("Вы уверены, что хотите удалить ${item.name} из списка?")
            .setPositiveButton("Да") { _, _ ->
                // Если подтверждено, удаляем элемент
                shoppingList.removeAt(position)
                originalShoppingList.remove(item)

                val itemRef = databaseReference.child(item.id)

                itemRef.removeValue()
                    .addOnSuccessListener {

                        filterShoppingList(currentCategory)
                        Toast.makeText(requireContext(), "Элемент удалён", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseError", "Ошибка удаления: ${exception.message}")
                        Toast.makeText(requireContext(), "Не удалось удалить элемент", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Нет") {_, _ -> updateShoppingList()}
            .show()
    }

    private fun showEditItemDialog(item: ShoppingItem) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_item, null)

        val editTextName = dialogView.findViewById<EditText>(R.id.etSearchProduct)
        editTextName.setText(item.name) // Заполняем текущие данные
        editTextName.setSelection(item.name.length)
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Редактировать продукт")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedName = editTextName.text.toString()
                if (updatedName.isNotEmpty()) {
                    item.name = updatedName
                    updateShoppingItem(item)
                } else {
                    Toast.makeText(requireContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun updateShoppingItem(item: ShoppingItem) {
        val itemRef = databaseReference.child(item.id) // Ссылка на элемент в Firebase

        itemRef.setValue(item)
            .addOnSuccessListener {
                adapter.notifyDataSetChanged()
                filterShoppingList(currentCategory)
                Toast.makeText(requireContext(), "Элемент обновлён", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Ошибка обновления: ${exception.message}")
                Toast.makeText(requireContext(), "Не удалось обновить элемент", Toast.LENGTH_SHORT).show()
            }
    }

    // Функция для перемещения элемента в холодильник
    private fun moveToFridge(item: ShoppingItem, position: Int) {
            showFreshnessDialog(item, position)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showFreshnessDialog(item: ShoppingItem, position: Int) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_freshness_check, null)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        val rgExpiryChoice = dialogView.findViewById<RadioGroup>(R.id.rgExpiryChoice)
        val layoutFreshnessStates = dialogView.findViewById<LinearLayout>(R.id.layoutFreshnessStates)
        val layoutManualDate = dialogView.findViewById<LinearLayout>(R.id.layoutManualDate)
        val spinnerFreshnessStates = dialogView.findViewById<Spinner>(R.id.spinnerFreshnessStates)
        val etExpiryDate = dialogView.findViewById<EditText>(R.id.etExpiryDate)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        // Настраиваем спиннер с состояниями свежести
        val options = when (item.category.lowercase()) {
            "фрукты", "овощи" -> arrayOf("Свежий", "Немного лежалый", "Перезрелый")
            "хлеб" -> arrayOf("Свежий", "Вчерашний")
            else -> arrayOf("Свежий", "Нормальный", "Требует скорого употребления")
        }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFreshnessStates.adapter = adapter

        // Обработчик переключения между режимами выбора срока
        rgExpiryChoice.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbFreshnessDefault -> {
                    layoutFreshnessStates.visibility = View.GONE
                    layoutManualDate.visibility = View.GONE
                }
                R.id.rbFreshnessState -> {
                    layoutFreshnessStates.visibility = View.VISIBLE
                    layoutManualDate.visibility = View.GONE
                }
                R.id.rbManualDate -> {
                    layoutFreshnessStates.visibility = View.GONE
                    layoutManualDate.visibility = View.VISIBLE
                }
            }
        }

        // Настраиваем выбор даты
        etExpiryDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    etExpiryDate.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            updateShoppingList()
        }

        btnConfirm.setOnClickListener {
            when (rgExpiryChoice.checkedRadioButtonId) {
                R.id.rbFreshnessDefault -> {
                    addToFridge(item, position, item.defaultStorageDays )
                }
                R.id.rbFreshnessState -> {
                    // Рассчитываем срок хранения на основе выбранного состояния
                    val storageDays = calculateStorageDays(item.defaultStorageDays, spinnerFreshnessStates.selectedItemPosition)
                    addToFridge(item, position, storageDays)
                }
                R.id.rbManualDate -> {
                    val dateStr = etExpiryDate.text.toString()
                    if (dateStr.isNotEmpty()) {
                        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        try {
                            val expiryDate = dateFormat.parse(dateStr)
                            val today = Calendar.getInstance().time
                            val diffInDays = ChronoUnit.DAYS.between(today.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                                expiryDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()) + 1
                            addToFridge(item, position, diffInDays.toInt())
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Неверный формат даты", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }
                    } else {
                        Toast.makeText(requireContext(), "Пожалуйста, выберите дату", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun calculateStorageDays(defaultDays: Int, freshnessLevel: Int): Int {
        return when (freshnessLevel) {
            0 -> defaultDays // Свежий - полный срок
            1 -> (defaultDays * 0.7).toInt() // Средний - 70% от срока
            2 -> (defaultDays * 0.3).toInt() // Требует скорого употребления - 30% от срока
            else -> defaultDays
        }
    }

    private fun addToFridge(item: ShoppingItem, position: Int, storageDays: Int) {
        // Получаем ссылки на базы данных
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fridgeRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(userId)
            .child("fridgeList")
        val shoppingRef = FirebaseDatabase.getInstance().getReference("Users")
            .child(userId)
            .child("shoppingList")

        // Создаем объект FridgeItem
        val fridgeItem = FridgeItem(
            id = item.id,
            name = item.name,
            category = item.category,
            imageUrl = item.imageUrl,
            defaultStorageDays = item.defaultStorageDays,
            addedDate = System.currentTimeMillis(),
            expiryDays = storageDays
        )
        // Копируем элемент в холодильник
        fridgeRef.child(item.id).setValue(fridgeItem).addOnSuccessListener {
            // После успешного копирования удаляем из списка покупок
            shoppingList.removeAt(position)
            shoppingRef.child(item.id).removeValue().addOnSuccessListener {
                Toast.makeText(context, "Товар перемещен в холодильник", Toast.LENGTH_SHORT).show()
                updateShoppingList()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка при удалении из списка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Ошибка при перемещении: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}