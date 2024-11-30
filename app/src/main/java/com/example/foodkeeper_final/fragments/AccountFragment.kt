package com.example.foodkeeper_final.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import com.example.foodkeeper_final.AuthActivity
import com.example.foodkeeper_final.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.fragment.app.Fragment

class AccountFragment : Fragment() {
    private lateinit var ivProfileImage: ImageView
    private lateinit var etName: EditText
    private lateinit var tvEmail: TextView
    private lateinit var btnSaveName: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnLogout: Button
    private lateinit var btnDeleteAccount: Button

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val userRef = database.getReference("Users")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_account, container, false)

        // Инициализация views
        ivProfileImage = view.findViewById(R.id.ivProfileImage)
        etName = view.findViewById(R.id.etName)
        tvEmail = view.findViewById(R.id.tvEmail)
        btnSaveName = view.findViewById(R.id.btnSaveName)
        btnChangePassword = view.findViewById(R.id.btnChangePassword)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)

        // Загрузка данных пользователя
        loadUserData()

        // Настройка обработчиков
        setupNameEditing()
        setupButtons()

        return view
    }

    private fun setupNameEditing() {
        // Показываем кнопку сохранения только при изменении текста
        etName.addTextChangedListener {
            btnSaveName.visibility = View.VISIBLE
        }

        // Обработка нажатия Done на клавиатуре
        etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveNameAndClearFocus()
                true
            } else {
                false
            }
        }

        btnSaveName.setOnClickListener {
            saveNameAndClearFocus()
        }
    }

    private fun saveNameAndClearFocus() {
        val name = etName.text.toString().trim()
        if (name.isNotEmpty()) {
            saveUserName(name)
            btnSaveName.visibility = View.GONE
            etName.clearFocus()
            // Скрываем клавиатуру
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(etName.windowToken, 0)
            showSuccessMessage("Имя успешно обновлено")
        } else {
            showErrorMessage("Имя не может быть пустым")
        }
    }

    private fun setupButtons() {
        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_change_password, null)
        val etCurrentPassword = dialogView.findViewById<EditText>(R.id.etCurrentPassword)
        val etNewPassword = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = dialogView.findViewById<EditText>(R.id.etConfirmPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Изменение пароля")
            .setView(dialogView)
            .setPositiveButton("Изменить") { dialog, _ ->
                val currentPassword = etCurrentPassword.text.toString()
                val newPassword = etNewPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (validatePasswordChange(currentPassword, newPassword, confirmPassword)) {
                    changePassword(currentPassword, newPassword)
                }
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun validatePasswordChange(currentPassword: String, newPassword: String, confirmPassword: String): Boolean {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showErrorMessage("Все поля должны быть заполнены")
            return false
        }

        if (newPassword.length < 6) {
            showErrorMessage("Новый пароль должен содержать минимум 6 символов")
            return false
        }

        if (newPassword != confirmPassword) {
            showErrorMessage("Пароли не совпадают")
            return false
        }

        if (newPassword == currentPassword) {
            showErrorMessage("Новый пароль должен отличаться от текущего")
            return false
        }

        return true
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            // Реаутентификация пользователя
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // Смена пароля
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            showSuccessMessage("Пароль успешно изменен")
                        }
                        .addOnFailureListener { e ->
                            showErrorMessage("Ошибка при смене пароля: ${e.message}")
                        }
                }
                .addOnFailureListener {
                    showErrorMessage("Неверный текущий пароль")
                }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Выход")
            .setMessage("Вы уверены, что хотите выйти?")
            .setPositiveButton("Да") { _, _ ->
                auth.signOut()
                startActivity(Intent(requireContext(), AuthActivity::class.java))
                requireActivity().finish()
            }
            .setNegativeButton("Нет", null)
            .show()
    }

    private fun showDeleteAccountConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление аккаунта")
            .setMessage("Вы уверены, что хотите удалить аккаунт? Это действие нельзя отменить.")
            .setPositiveButton("Удалить") { _, _ ->
                showPasswordConfirmationDialog()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showPasswordConfirmationDialog() {
        val input = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "Введите пароль для подтверждения"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Подтверждение")
            .setView(input)
            .setPositiveButton("Подтвердить") { _, _ ->
                val password = input.text.toString()
                if (password.isNotEmpty()) {
                    deleteAccount(password)
                } else {
                    showErrorMessage("Введите пароль")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteAccount(password: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, password)
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // Удаляем данные пользователя из базы данных
                    userRef.child(user.uid).removeValue()
                        .addOnSuccessListener {
                            // Удаляем аккаунт
                            user.delete()
                                .addOnSuccessListener {
                                    startActivity(Intent(requireContext(), AuthActivity::class.java))
                                    requireActivity().finish()
                                }
                                .addOnFailureListener { e ->
                                    showErrorMessage("Ошибка при удалении аккаунта: ${e.message}")
                                }
                        }
                        .addOnFailureListener { e ->
                            showErrorMessage("Ошибка при удалении данных: ${e.message}")
                        }
                }
                .addOnFailureListener {
                    showErrorMessage("Неверный пароль")
                }
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            tvEmail.text = auth.currentUser?.email ?: ""
            userRef.child(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    etName.setText(name ?: "")
                    btnSaveName.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    showErrorMessage("Ошибка загрузки данных")
                }
            })
        }
    }

    private fun saveUserName(name: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            userRef.child(userId).child("name").setValue(name)
                .addOnFailureListener {
                    showErrorMessage("Ошибка сохранения имени")
                }
        }
    }

    private fun showSuccessMessage(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(resources.getColor(android.R.color.holo_green_dark))
            .setTextColor(resources.getColor(android.R.color.white))
            .show()
    }

    private fun showErrorMessage(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(resources.getColor(android.R.color.holo_red_dark))
            .setTextColor(resources.getColor(android.R.color.white))
            .show()
    }
}
