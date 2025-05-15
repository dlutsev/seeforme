package com.example.seeforme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val CHANNEL_ID_CALLS = "help_channel"
        private const val CHANNEL_NAME_CALLS = "Запросы помощи"
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Сохраняем токен в SharedPreferences
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("fcm_token", token)
        editor.apply()
        
        Log.d("FCM", "New token: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d("FCM", "Message received: ${message.data}")
        
        // Проверяем тип уведомления
        if (message.data.containsKey("type") && message.data["type"] == "call_request") {
            // Это запрос на звонок
            val callerName = message.data["caller_name"] ?: "Неизвестный пользователь"
            showCallNotification(callerName)
        } else if (message.notification != null) {
            // Обычное уведомление
            showDefaultNotification(
                message.notification?.title,
                message.notification?.body
            )
        }
    }

    private fun showCallNotification(callerName: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel(notificationManager)
        
        // Создаем интент для перехода на экран принятия вызова
        val intent = Intent(this, AcceptCallActivity::class.java)
        intent.putExtra("caller_name", callerName)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_CALLS)
            .setContentTitle("Входящий вызов")
            .setContentText("$callerName запрашивает помощь")
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // Показывать поверх экрана блокировки
            
        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }

    private fun showDefaultNotification(title: String?, body: String?) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        createNotificationChannel(notificationManager)
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID_CALLS)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)

        notificationManager.notify(Random.nextInt(), notificationBuilder.build())
    }
    
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_CALLS,
                CHANNEL_NAME_CALLS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для уведомлений о входящих запросах помощи"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}