package com.example.foodkeeper_final

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.foodkeeper_final.databinding.ActivityAuthBinding
import com.example.foodkeeper_final.models.User
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AuthActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Очищаем сохраненный ID при входе в AuthActivity
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .edit()
            .remove("selected_user_id")
            .apply()

        // Обработчики нажатий
        binding.btnLogin?.setOnClickListener { loginWithEmailAndPassword() }
        binding.tvRegister?.setOnClickListener { registerUser() }
        binding.tvForgotPassword?.setOnClickListener { resetPassword() }
    }

    // Авторизация по почте и паролю
    private fun loginWithEmailAndPassword() {
        val email = binding.etEmail?.text.toString().trim()
        val password = binding.etPassword?.text.toString().trim()

        // Валидация полей
        if (email.isEmpty()) {
            showErrorSnackbar("Введите email для входа")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorSnackbar("Введите корректный email адрес")
            return
        }

        if (password.isEmpty()) {
            showErrorSnackbar("Введите пароль")
            return
        }

        // Показываем прогресс
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Выполняется вход...")
            show()
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                progressDialog.dismiss()
                if (task.isSuccessful) {
                    checkFamilyMembership()
                } else {
                    when (task.exception) {
                        is FirebaseAuthInvalidUserException ->
                            showErrorSnackbar("Пользователь с таким email не найден")
                        is FirebaseAuthInvalidCredentialsException ->
                            showErrorSnackbar("Неверный email или пароль")
                        is FirebaseTooManyRequestsException ->
                            showErrorSnackbar("Слишком много попыток входа. Попробуйте позже")
                        else -> showErrorSnackbar("Ошибка входа: ${task.exception?.message}")
                    }
                }
            }
    }

    // Регистрация пользователя
    private fun registerUser() {
        val email = binding.etEmail?.text.toString().trim()
        val password = binding.etPassword?.text.toString().trim()

        // Валидация полей
        if (email.isEmpty()) {
            showErrorSnackbar("Введите email для регистрации")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorSnackbar("Введите корректный email адрес")
            return
        }

        if (password.isEmpty()) {
            showErrorSnackbar("Введите пароль")
            return
        }

        if (password.length < 6) {
            showErrorSnackbar("Пароль должен содержать минимум 6 символов")
            return
        }

        // Показываем прогресс
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Выполняется регистрация...")
            show()
        }

        // Сначала проверяем, не существует ли уже пользователь с таким email
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { checkTask ->
            if (checkTask.isSuccessful) {
                val signInMethods = checkTask.result?.signInMethods ?: emptyList<String>()
                if (signInMethods.isNotEmpty()) {
                    progressDialog.dismiss()
                    showErrorSnackbar("Пользователь с таким email уже зарегистрирован")
                    return@addOnCompleteListener
                }

                // Если email свободен, регистрируем пользователя
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        progressDialog.dismiss()
                        if (task.isSuccessful) {
                            Snackbar.make(binding.root, "Регистрация успешна", Snackbar.LENGTH_SHORT).show()
                            addUserToDatabase()
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        } else {
                            when (task.exception) {
                                is FirebaseAuthWeakPasswordException ->
                                    showErrorSnackbar("Пароль слишком простой. Используйте более сложный пароль")
                                is FirebaseAuthInvalidCredentialsException ->
                                    showErrorSnackbar("Некорректный формат email")
                                is FirebaseTooManyRequestsException ->
                                    showErrorSnackbar("Слишком много попыток. Попробуйте позже")
                                else -> showErrorSnackbar("Ошибка регистрации: ${task.exception?.message}")
                            }
                        }
                    }
            } else {
                progressDialog.dismiss()
                showErrorSnackbar("Ошибка проверки email: ${checkTask.exception?.message}")
            }
        }
    }

    // Функция для сброса пароля
    private fun resetPassword() {
        val email = binding.etEmail?.text.toString().trim()

        // Валидация email
        if (email.isEmpty()) {
            showErrorSnackbar("Введите email для восстановления пароля")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorSnackbar("Введите корректный email адрес")
            return
        }

        // Показываем прогресс
        val progressDialog = ProgressDialog(this).apply {
            setMessage("Отправка инструкций для восстановления пароля...")
            show()
        }

        // Отправляем письмо для сброса пароля
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { resetTask ->
                progressDialog.dismiss()
                if (resetTask.isSuccessful) {
                    Snackbar.make(
                        binding.root,
                        "Если пользователь с таким email существует, инструкции по сбросу пароля будут отправлены",
                        Snackbar.LENGTH_LONG
                    ).show()
                } else {
                    when (resetTask.exception) {
                        is FirebaseAuthInvalidUserException ->
                            showErrorSnackbar("Пользователь с таким email не найден")
                        is FirebaseAuthInvalidCredentialsException ->
                            showErrorSnackbar("Некорректный формат email")
                        is FirebaseTooManyRequestsException ->
                            showErrorSnackbar("Слишком много попыток. Попробуйте позже")
                        else -> showErrorSnackbar("Ошибка: ${resetTask.exception?.message}")
                    }
                }
            }
    }

    // Функция для отображения ошибок с помощью Snackbar
    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") {}
            .setTextColor(getResources().getColor(android.R.color.white))
            .setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark))
            .setActionTextColor(getResources().getColor(android.R.color.white))
            .show()
    }

    private fun addUserToDatabase() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val user = User(
                email = auth.currentUser?.email ?: "Не указан",
                shoppingList = emptyList(),
                fridgeList = emptyList()
            )

            val database = FirebaseDatabase.getInstance()
            val userRef = database.reference.child("Users").child(userId)

            userRef.setValue(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Snackbar.make(binding.root, "Данные пользователя добавлены в базу", Snackbar.LENGTH_SHORT).show()
                    } else {
                        showErrorSnackbar("Ошибка при добавлении данных пользователя: ${task.exception?.message}")
                    }
                }
        } else {
            showErrorSnackbar("Ошибка: пользователь не авторизован")
        }
    }

    private fun checkFamilyMembership() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showErrorSnackbar("Ошибка: пользователь не авторизован")
            return
        }

        val database = FirebaseDatabase.getInstance()
        val usersRef = database.reference.child("Users")

        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var familyOwnerData: Pair<String, String>? = null // Pair of userId and email

                // Ищем пользователя в семейных списках
                for (userSnapshot in snapshot.children) {
                    val familySnapshot = userSnapshot.child("family")
                    for (familyMember in familySnapshot.children) {
                        val memberEmail = familyMember.child("email").getValue(String::class.java)
                        if (memberEmail == currentUser.email) {
                            val ownerEmail = userSnapshot.child("email").getValue(String::class.java)
                            familyOwnerData = Pair(userSnapshot.key ?: "", ownerEmail ?: "")
                            break
                        }
                    }
                    if (familyOwnerData != null) break
                }

                if (familyOwnerData != null) {
                    showAccountChoiceDialog(familyOwnerData)
                } else {
                    proceedToMainActivity(currentUser.uid)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                showErrorSnackbar("Ошибка при проверке семейного доступа: ${error.message}")
                proceedToMainActivity(currentUser.uid)
            }
        })
    }

    private fun showAccountChoiceDialog(familyOwnerData: Pair<String, String>) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showErrorSnackbar("Ошибка: пользователь не авторизован")
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Выберите аккаунт")
            .setMessage("Вы являетесь членом семьи пользователя ${familyOwnerData.second}")
            .setPositiveButton("Войти в семейный список") { _, _ ->
                proceedToMainActivity(familyOwnerData.first)
            }
            .setNegativeButton("Войти в личный список") { _, _ ->
                proceedToMainActivity(currentUser.uid)
            }
            .setCancelable(false)
            .create()
            .show()
    }

    private fun proceedToMainActivity(userId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("SELECTED_USER_ID", userId)
        }
        startActivity(intent)
        finish()
    }
}
