package com.example.foodkeeper_final.fragments

import android.app.AlertDialog
import android.app.DatePickerDialog
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
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodkeeper_final.R
import com.example.foodkeeper_final.adapters.FridgeAdapter
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

class FridgeFragment<T> : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddItem: FloatingActionButton
    private lateinit var adapter: FridgeAdapter
    private val fridgeList = mutableListOf<FridgeItem>()
    private val recentProductsList = mutableListOf<String>()
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private val suggestionsList = mutableListOf<String>()
    private val originalFridgeList = mutableListOf<FridgeItem>() // Исходный список
    private var currentCategory: String = "Все" // По умолчанию категория "Все"


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fridge, container, false)

        // Инициализация RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewFridge)
        fabAddItem = view.findViewById(R.id.fabAddItem)
        adapter = FridgeAdapter(
            fridgeList,
            onEdit = { item -> showEditItemDialog(item) },
            onDelete = { item -> deleteFridgeItem(item) },
            onItemClick = { item -> showProductDetailsDialog(item) } // Новый обработчик для клика на изображение
        )
        recyclerView.layoutManager = GridLayoutManager(context, 4) // 3 элемента в строке
        recyclerView.adapter = adapter


        val spinnerCategory: Spinner = view.findViewById(R.id.spinnerCategory)

        // Настройка Spinner
        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                currentCategory = parent.getItemAtPosition(position).toString()
                filterFridgeList(currentCategory) // Применяем фильтр
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Инициализация Firestore
        firestore = FirebaseFirestore.getInstance()

        // Инициализация RealtimeDatabase
        databaseReference = getUserDatabaseReference()

        // Загрузка списка покупок текущего пользователя
        loadUserFridgeList()
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
            .child("fridgeList")
    }

    // Загрузка списка покупок из Firebase Realtime Database
    private fun loadUserFridgeList() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fridgeList.clear()
                originalFridgeList.clear()

                if (snapshot.exists()) {
                    for (itemSnapshot in snapshot.children) {
                        val fridgeItem = itemSnapshot.getValue(FridgeItem::class.java)
                        if (fridgeItem != null) {
                            fridgeList.add(fridgeItem)
                            originalFridgeList.add(fridgeItem)
                        }
                    }
                }

                if (fridgeList.isEmpty()) {
                    showEmptyListMessage()
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Ошибка загрузки холодильника: ${error.message}")
            }
        })
    }

    private fun updateFridgeList() {
        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fridgeList.clear()
                originalFridgeList.clear()

                if (snapshot.exists()) {
                    for (itemSnapshot in snapshot.children) {
                        val fridgeItem = itemSnapshot.getValue(FridgeItem::class.java)
                        if (fridgeItem != null) {
                            fridgeList.add(fridgeItem)
                            originalFridgeList.add(fridgeItem)
                        }
                    }
                }

                if (fridgeList.isEmpty()) {
                    showEmptyListMessage()
                }
                filterFridgeList(currentCategory)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseError", "Ошибка загрузки списка: ${error.message}")
            }
        })
    }

    private fun filterFridgeList(category: String) {
        fridgeList.clear()
        if (category == "Все") {
            fridgeList.addAll(originalFridgeList) // Возвращаем все элементы
        } else {
            fridgeList.addAll(originalFridgeList.filter { it.category == category }) // Фильтруем по категории
        }
        adapter.notifyDataSetChanged() // Обновляем адаптер
    }

    // Метод для отображения сообщения, что холодильник пуст
    private fun showEmptyListMessage() {
        if (isAdded) {  // Проверка, прикреплен ли фрагмент к активности
            Toast.makeText(requireContext(), "Ваш холодильник пуст. Добавьте товары!", Toast.LENGTH_SHORT).show()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
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
        filterFridgeList(currentCategory)
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
                    showFreshnessDialog(selectedProduct)

                    // Обновляем список последних продуктов
                    if (!recentProductsList.contains(selectedProductName)) {
                        recentProductsList.add(selectedProductName)
                        if (recentProductsList.size > 5) { // Ограничиваем размер списка
                            recentProductsList.removeAt(0)
                        }
                    }
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

    private fun loadProductFromFirestore(productName: String, callback: (FridgeItem?) -> Unit) {
        val lowerCaseProductName = productName.lowercase()

        firestore.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                // Ищем среди всех документов
                for (document in documents) {
                    val product = document.toObject(FridgeItem::class.java)
                    val productNameInDoc = product.name?.lowercase()

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
    private fun searchProducts(query: String, callback: (List<FridgeItem>) -> Unit) {
        firestore.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                val suggestions = mutableListOf<FridgeItem>()

                for (document in documents) {
                    val product = document.toObject(FridgeItem::class.java)
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
    private fun addFridgeItem(item: FridgeItem) {

        // Создаем объект FridgeItem
        val fridgeItem = FridgeItem(
            id = item.id,
            name = item.name,
            category = item.category,
            imageUrl = item.imageUrl,
            defaultStorageDays = item.defaultStorageDays,
            addedDate = System.currentTimeMillis(),
            expiryDays = item.expiryDays
        )

        val itemRef = databaseReference.push() // Генерация уникального ключа
        fridgeItem.id = itemRef.key ?: return // Устанавливаем ID товара

        itemRef.setValue(fridgeItem)
            .addOnSuccessListener {
                updateFridgeList()
                Log.d("Firebase", "Продукт добавлен: ${fridgeItem.name}")
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Ошибка добавления: ${exception.message}")
                Toast.makeText(requireContext(), "Не удалось добавить продукт", Toast.LENGTH_SHORT).show()
            }
    }

    // Удаление элемента из списка м
    private fun deleteFridgeItem(item: FridgeItem) {
        // Проверка, чтобы избежать выхода за пределы массива

        // Создаем диалоговое окно для подтверждения удаления
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить товар?")
            .setMessage("Вы уверены, что хотите удалить ${item.name} из списка?")
            .setPositiveButton("Да") { _, _ ->
                // Если подтверждено, удаляем элемент
                fridgeList.removeIf { it.name == item.name }
                originalFridgeList.remove(item)

                val itemRef = databaseReference.child(item.id)

                itemRef.removeValue()
                    .addOnSuccessListener {

                        filterFridgeList(currentCategory)
                        Toast.makeText(requireContext(), "Элемент удалён", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseError", "Ошибка удаления: ${exception.message}")
                        Toast.makeText(requireContext(), "Не удалось удалить элемент", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Нет") {_, _ -> updateFridgeList()}
            .show()
    }

    private fun showEditItemDialog(item: FridgeItem) {
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
                    updateFridgeItem(item)
                } else {
                    Toast.makeText(requireContext(), "Название не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun updateFridgeItem(item: FridgeItem) {
        val itemRef = databaseReference.child(item.id) // Ссылка на элемент в Firebase

        itemRef.setValue(item)
            .addOnSuccessListener {
                adapter.notifyDataSetChanged()
                filterFridgeList(currentCategory)
                Toast.makeText(requireContext(), "Элемент обновлён", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Ошибка обновления: ${exception.message}")
                Toast.makeText(requireContext(), "Не удалось обновить элемент", Toast.LENGTH_SHORT).show()
            }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun showProductDetailsDialog(item: FridgeItem) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_product_details, null)

        val textViewProductName = dialogView.findViewById<TextView>(R.id.tvProductName)
        val textViewProductCategory = dialogView.findViewById<TextView>(R.id.tvProductCategory)
        val textViewProductExpiry = dialogView.findViewById<TextView>(R.id.tvProductExpiry)
        val btnEditProduct = dialogView.findViewById<Button>(R.id.btnEditProduct)
        val btnDeleteProduct = dialogView.findViewById<Button>(R.id.btnDeleteProduct)
        val btnMoveToShoppingList = dialogView.findViewById<Button>(R.id.btnMoveToShoppingList)

        textViewProductName.text = item.name
        textViewProductCategory.text = "Категория: ${item.category}"
        val today = LocalDate.now()
        val expiryDate = today.plusDays(item.expiryDays.toLong() - 1)
        textViewProductExpiry.text = "Срок годности до: ${expiryDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))}"


        val dialog = AlertDialog.Builder(context)
            .setTitle("Информация о продукте")
            .setView(dialogView)
            .setNegativeButton("Закрыть", null)
            .create()

        // Обработчик кнопки "Редактировать"
        btnEditProduct.setOnClickListener {
            dialog.dismiss() // Закрываем диалог
            showEditItemDialog(item) // Открываем диалог редактирования
        }

        // Обработчик кнопки "Удалить"
        btnDeleteProduct.setOnClickListener {
            dialog.dismiss() // Закрываем диалог
            deleteFridgeItem(item) // Удаляем элемент
        }

        btnMoveToShoppingList.setOnClickListener {
            dialog.dismiss() // Закрываем диалог
            moveItemToShoppingList(item) // Перемещаем элемент
        }

        dialog.show()
    }

    private fun moveItemToShoppingList(item: FridgeItem) {
        val shoppingListRef = databaseReference.parent?.child("shoppingList") // Ссылка на список покупок

        if (shoppingListRef == null) {
            Log.e("FirebaseError", "Не удалось получить ссылку на shoppingList")
            return
        }


        // Создаем объект ShoppingItem
        val shoppingItem = ShoppingItem(
            id = item.id,
            name = item.name,
            category = item.category,
            imageUrl = item.imageUrl,
            defaultStorageDays = item.defaultStorageDays
        )

        // Создаем элемент в списке покупок с тем же ключом
        val shoppingItemRef = shoppingListRef.child(shoppingItem.id)
        shoppingItemRef.setValue(shoppingItem)
            .addOnSuccessListener {
                // После успешного добавления удаляем из холодильника
                fridgeList.removeIf { it.name == shoppingItem.name }
                filterFridgeList(currentCategory)
                originalFridgeList.remove(item)
                updateFridgeList()
                databaseReference.child(item.id).removeValue()
                    .addOnSuccessListener {
                        filterFridgeList(currentCategory)
                        Toast.makeText(requireContext(), "Элемент перемещён в список покупок", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("FirebaseError", "Ошибка удаления из холодильника: ${exception.message}")
                        Toast.makeText(requireContext(), "Ошибка при удалении из холодильника", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseError", "Ошибка перемещения в список покупок: ${exception.message}")
                Toast.makeText(requireContext(), "Ошибка при добавлении в список покупок", Toast.LENGTH_SHORT).show()
            }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showFreshnessDialog(item: FridgeItem) {
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
            updateFridgeList()
        }

        btnConfirm.setOnClickListener {
            when (rgExpiryChoice.checkedRadioButtonId) {
                R.id.rbFreshnessDefault -> {
                    item.expiryDays = item.defaultStorageDays
                    addFridgeItem(item)
                }
                R.id.rbFreshnessState -> {
                    // Рассчитываем срок хранения на основе выбранного состояния
                    val storageDays = calculateStorageDays(item.defaultStorageDays, spinnerFreshnessStates.selectedItemPosition)
                    item.expiryDays = storageDays
                    addFridgeItem(item)
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
                            item.expiryDays = diffInDays.toInt()
                            addFridgeItem(item)

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


}