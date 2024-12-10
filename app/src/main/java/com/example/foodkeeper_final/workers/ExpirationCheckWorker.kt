package com.example.foodkeeper_final.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.foodkeeper_final.MainActivity
import com.example.foodkeeper_final.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ExpirationCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    private val CHANNEL_ID = "FoodKeeper_Notifications"
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        fun testNotifications(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<ExpirationCheckWorker>()
                .build()
            
            WorkManager.getInstance(context)
                .enqueue(workRequest)
        }
    }

    override fun doWork(): Result {
        try {
            val currentUser = getCurrentUserId()
            if (currentUser != null) {
                checkExpirationDates(currentUser)
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun checkExpirationDates(userId: String) {
        val fridgeRef = database.getReference("Users").child(userId).child("fridgeList")
        
        fridgeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notificationsByDay = mutableMapOf<Int, MutableList<String>>()
                
                for (itemSnapshot in snapshot.children) {
                    val name = itemSnapshot.child("name").getValue(String::class.java) ?: continue
                    val expiryDays = itemSnapshot.child("expiryDays").getValue(Int::class.java) ?: continue
                    
                    when {
                        expiryDays <= 0 -> {
                            notificationsByDay.getOrPut(0) { mutableListOf() }.add(name)
                        }
                        expiryDays == 1 -> {
                            notificationsByDay.getOrPut(1) { mutableListOf() }.add(name)
                        }
                        expiryDays == 3 -> {
                            notificationsByDay.getOrPut(3) { mutableListOf() }.add(name)
                        }
                    }
                }
                
                // Отправляем групповые уведомления
                notificationsByDay.forEach { (days, products) ->
                    when (days) {
                        0 -> showNotification(
                            "Срок годности истек!",
                            "Истек срок годности: ${products.joinToString(", ")}"
                        )
                        1 -> showNotification(
                            "Срок годности истекает завтра",
                            "Завтра истекает срок годности: ${products.joinToString(", ")}"
                        )
                        3 -> showNotification(
                            "Срок годности скоро истекает",
                            "Через 3 дня истекает срок годности: ${products.joinToString(", ")}"
                        )
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Обработка ошибок
            }
        })
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun getCurrentUserId(): String? {
        val currentUser = auth.currentUser
        return if (currentUser != null && currentUser.uid.isNotEmpty()) {
            currentUser.uid
        } else {
            // Если пользователь не авторизован, отменяем работу
            WorkManager.getInstance(context).cancelAllWorkByTag("expiration_check")
            null
        }
    }
}
