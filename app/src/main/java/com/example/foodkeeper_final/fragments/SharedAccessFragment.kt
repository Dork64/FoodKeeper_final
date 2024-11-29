package com.example.foodkeeper_final.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.foodkeeper_final.databinding.FragmentSharedAccessBinding

class SharedAccessFragment : Fragment() {
    private var _binding: FragmentSharedAccessBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSharedAccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        binding.addFamilyButton.setOnClickListener {
            Toast.makeText(context, "Член семьи добавлен", Toast.LENGTH_SHORT).show()
        }

        binding.removeFamilyButton.setOnClickListener {
            Toast.makeText(context, "Член семьи удален", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
