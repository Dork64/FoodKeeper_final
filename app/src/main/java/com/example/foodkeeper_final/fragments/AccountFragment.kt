package com.example.foodkeeper_final.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.foodkeeper_final.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AccountFragment : Fragment() {
    private lateinit var ivProfileImage: ImageView
    private lateinit var etName: EditText
    private lateinit var tvEmail: TextView

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

        // Загрузка данных пользователя
        loadUserData()

        // Обработчик изменения имени
        etName.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                saveUserName(etName.text.toString())
            }
        }

        return view
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Загружаем email
            tvEmail.text = auth.currentUser?.email ?: ""

            // Загружаем данные из Realtime Database
            userRef.child(userId).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    etName.setText(name ?: "")
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun saveUserName(name: String) {
        val userId = auth.currentUser?.uid
        if (userId != null && name.isNotEmpty()) {
            userRef.child(userId).child("name").setValue(name)
                .addOnFailureListener {
                    Toast.makeText(context, "Ошибка сохранения имени", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
