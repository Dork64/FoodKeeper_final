package com.example.foodkeeper_final.fragments

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.findNavController
import com.example.foodkeeper_final.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Настройка навигации
        view.findViewById<Button>(R.id.btnAccount).setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_account)
        }

        view.findViewById<Button>(R.id.btnSettings).setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }

        view.findViewById<Button>(R.id.btnAppearance).setOnClickListener {
            showThemeDialog()
        }

        view.findViewById<Button>(R.id.btnSharing).setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_sharedAccess)
        }

        view.findViewById<Button>(R.id.btnHelp).setOnClickListener {
            // TODO: Implement help section
        }

        return view
    }

    private fun showThemeDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.dialog_theme_chooser)
        
        val radioGroup = dialog.findViewById<RadioGroup>(R.id.themeRadioGroup)
        val lightThemeRadio = dialog.findViewById<RadioButton>(R.id.lightThemeRadio)
        val darkThemeRadio = dialog.findViewById<RadioButton>(R.id.darkThemeRadio)

        // Устанавливаем текущую тему
        val prefs = requireContext().getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val isDarkTheme = prefs.getBoolean("dark_theme", false)
        if (isDarkTheme) {
            darkThemeRadio.isChecked = true
        } else {
            lightThemeRadio.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.lightThemeRadio -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    prefs.edit().putBoolean("dark_theme", false).apply()
                }
                R.id.darkThemeRadio -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    prefs.edit().putBoolean("dark_theme", true).apply()
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}