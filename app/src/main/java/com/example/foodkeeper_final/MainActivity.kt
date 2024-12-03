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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Загружаем и применяем сохранённую тему
        val prefs = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDarkTheme = prefs.getBoolean("dark_theme", false)
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        // Проверяем авторизацию до установки интерфейса
        if (FirebaseAuth.getInstance().currentUser == null) {
            // Если пользователь не авторизован - отправляем на экран входа
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
}