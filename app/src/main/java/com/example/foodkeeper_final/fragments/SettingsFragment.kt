package com.example.foodkeeper_final.fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import com.google.android.material.switchmaterial.SwitchMaterial
import com.example.foodkeeper_final.R

class SettingsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val autoSortSwitch = view.findViewById<SwitchMaterial>(R.id.autoSortSwitch)
        val prefs = requireContext().getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        autoSortSwitch.isChecked = prefs.getBoolean("auto_sort", false)

        autoSortSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_sort", isChecked).apply()
        }

        val autoTransferSwitch = view.findViewById<SwitchMaterial>(R.id.autoTransferSwitch)
        autoTransferSwitch.isChecked = prefs.getBoolean("auto_transfer", false)

        autoTransferSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_transfer", isChecked).apply()
        }

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        // Переход к экрану уведомлений
        view.findViewById<View>(R.id.notificationsCard).setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_notifications)
        }

        return view
    }

}
