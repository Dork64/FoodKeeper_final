package com.example.foodkeeper_final

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.foodkeeper_final.databinding.ActivityAuthBinding
import com.example.foodkeeper_final.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.FirebaseDatabase

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Обработчики нажатий
        binding.btnLogin?.setOnClickListener { loginWithEmailAndPassword() }
        binding.tvRegister?.setOnClickListener { registerUser() }
    }

    // Авторизация по почте и паролю
    private fun loginWithEmailAndPassword() {
        val email = binding.etEmail?.text.toString().trim()
        val password = binding.etPassword?.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorSnackbar("Пожалуйста, введите корректный email!")
            return
        }

        if (password.isEmpty()) {
            showErrorSnackbar("Пожалуйста, введите пароль!")
            return
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Snackbar.make(binding.root, "Авторизация успешна", Snackbar.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showErrorSnackbar("Ошибка: ${task.exception?.message}")
                }
            }
    }

    // Регистрация пользователя
    private fun registerUser() {
        val email = binding.etEmail?.text.toString().trim()
        val password = binding.etPassword?.text.toString().trim()

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorSnackbar("Пожалуйста, введите корректный email!")
            return
        }

        if (password.isEmpty() || password.length < 6) {
            showErrorSnackbar("Пароль должен быть не менее 6 символов!")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Snackbar.make(binding.root, "Регистрация успешна", Snackbar.LENGTH_SHORT).show()
                    addUserToDatabase()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    showErrorSnackbar("Ошибка: ${task.exception?.message}")
                }
            }
    }

    // Функция для отображения ошибок с помощью Snackbar
    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("OK") {}
            .setTextColor(getResources().getColor(android.R.color.white))
            .setBackgroundTint(getResources().getColor(android.R.color.holo_red_dark))
            .show()
    }

    private fun addUserToDatabase() {
        val userId = auth.currentUser?.uid // Получаем UID текущего пользователя
        // Проверка, что пользователь авторизован
        if (userId != null) {
            val user = User(
                email = auth.currentUser?.email ?: "Не указан",
                shoppingList = emptyList(),
                fridgeList = emptyList()
            )

            // Получаем ссылку на базу данных
            val database = FirebaseDatabase.getInstance()
            val userRef = database.reference.child("Users").child(userId) // Пользователь сохраняется по его userId

            // Добавляем данные пользователя в базу данных
            userRef.setValue(user)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Успешно добавлено в базу данных
                        Snackbar.make(binding.root, "Данные пользователя добавлены в базу", Snackbar.LENGTH_SHORT).show()
                    } else {
                        // Ошибка при добавлении
                        showErrorSnackbar("Ошибка при добавлении данных пользователя в базу: ${task.exception?.message}")
                    }
                }
        } else {
            showErrorSnackbar("Пользователь не авторизован")
        }
    }
}
