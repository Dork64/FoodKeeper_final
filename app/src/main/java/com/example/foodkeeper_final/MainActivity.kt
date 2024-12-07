package com.example.foodkeeper_final

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNav: BottomNavigationView
    
    companion object {
        var CURRENT_USER_ID: String? = null
            set(value) {
                field = value
                // При изменении ID сохраняем его в SharedPreferences
                value?.let { id ->
                    instance?.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        ?.edit()
                        ?.putString("selected_user_id", id)
                        ?.apply()
                }
            }
        private var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        
        FirebaseApp.initializeApp(this)

        // Загружаем и применяем сохранённую тему
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDarkTheme = prefs.getBoolean("dark_theme", false)
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Получаем выбранный userId из Intent или из SharedPreferences
        CURRENT_USER_ID = intent.getStringExtra("SELECTED_USER_ID") ?: run {
            getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("selected_user_id", null)
        }

        // Проверяем авторизацию до установки интерфейса
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null || CURRENT_USER_ID == null) {
            // Если пользователь не авторизован или нет выбранного userId - отправляем на экран входа
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        // Если пользователь авторизован - показываем основной интерфейс
        setContentView(R.layout.activity_main)
        
        bottomNav = findViewById(R.id.bottomNav)

        // Получаем NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Связываем BottomNavigationView с NavController
        bottomNav.setupWithNavController(navController)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            instance = null
        }
    }
}