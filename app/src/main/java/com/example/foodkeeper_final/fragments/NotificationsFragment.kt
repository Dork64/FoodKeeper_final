package com.example.foodkeeper_final.fragments

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.foodkeeper_final.R
import java.util.*
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.foodkeeper_final.workers.ExpirationCheckWorker
import java.util.concurrent.TimeUnit

class NotificationsFragment : Fragment() {
    private lateinit var prefs: SharedPreferences
    private val CHANNEL_ID = "FoodKeeper_Notifications"
    private val PERMISSION_REQUEST_CODE = 123

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)
        prefs = requireActivity().getSharedPreferences("notifications", Context.MODE_PRIVATE)

        // Настройка кнопки назад
        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            findNavController().navigateUp()
        }

        // Создаем канал уведомлений
        createNotificationChannel()

        // Настройка переключателей
        setupSwitches(view)

        // Настройка кнопки включения уведомлений
        view.findViewById<Button>(R.id.btnEnableNotifications).setOnClickListener {
            requestNotificationPermission()
        }

        // Настройка кнопки тестирования уведомлений
        view.findViewById<Button>(R.id.btnTestNotifications).setOnClickListener {
            ExpirationCheckWorker.testNotifications(requireContext())
        }

        return view
    }

    private fun setupSwitches(view: View) {
        val pushSwitch = view.findViewById<SwitchCompat>(R.id.switchPushNotifications)

        // Устанавливаем начальные значения
        pushSwitch.isChecked = prefs.getBoolean("push_notifications", false)

        // Обработчики изменений
        pushSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("push_notifications", isChecked).apply()
            if (isChecked) {
                scheduleExpirationChecks()
            } else {
                WorkManager.getInstance(requireContext()).cancelAllWorkByTag("expiration_check")
            }
        }

    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "FoodKeeper Notifications"
            val descriptionText = "Notifications about food expiration and family changes"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            } else {
                enableNotifications()
            }
        } else {
            enableNotifications()
        }
    }

    private fun enableNotifications() {
        val pushSwitch = view?.findViewById<SwitchCompat>(R.id.switchPushNotifications)
        pushSwitch?.isChecked = true
        scheduleExpirationChecks()
    }

    private fun scheduleExpirationChecks() {
        val workManager = WorkManager.getInstance(requireContext())
        if (workManager == null) {
            // Инициализируем WorkManager, если он еще не инициализирован
            val config = Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO)
                .build()
            WorkManager.initialize(requireContext(), config)
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        // Проверка каждые 8 часов
        val periodicWork = PeriodicWorkRequestBuilder<ExpirationCheckWorker>(8, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("expiration_check")
            .build()

        WorkManager.getInstance(requireContext())
            .enqueueUniquePeriodicWork(
                "expiration_check",
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicWork
            )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableNotifications()
                }
            }
        }
    }
}
