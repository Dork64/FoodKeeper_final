package com.example.foodkeeper_final

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.foodkeeper_final.fragments.ShoppingListFragment
import androidx.fragment.app.Fragment
import com.example.foodkeeper_final.fragments.FridgeFragment
import com.example.foodkeeper_final.fragments.ProfileFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    lateinit var bottomNav : BottomNavigationView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)
        loadFragment(ShoppingListFragment<Any>())
        bottomNav = findViewById(R.id.bottomNav) as BottomNavigationView
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.shoppingList -> {
                    loadFragment(ShoppingListFragment<Any>())
                    true
                }
                R.id.fridge -> {
                    loadFragment(FridgeFragment())
                    true
                }
                R.id.profile -> {
                    loadFragment(ProfileFragment())
                    true
                }

                else -> {
                    loadFragment(ShoppingListFragment<Any>())
                    true
                }
            }
        }
    }
    private  fun loadFragment(fragment: Fragment){
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container,fragment)
        transaction.commit()
    }
}