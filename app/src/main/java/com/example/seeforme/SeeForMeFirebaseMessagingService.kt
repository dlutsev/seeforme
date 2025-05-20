package com.example.seeforme

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SeeForMeFirebaseMessagingService : FirebaseMessagingService() {
    
    private val TAG = "FirebaseMsgService"
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Получено сообщение: ${remoteMessage.data}")
        sendCallNotification()

        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Данные сообщения: ${remoteMessage.data}")
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Получен новый FCM токен: $token")
    }
    
    private fun sendCallNotification() {
        val channelId = "help_requests_channel"
        val intent = Intent(this, VolunteerMainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("from_notification", true)
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, pendingIntentFlags
        )
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Запрос о помощи")
            .setContentText("Пользователь запрашивает помощь. Нажмите, чтобы ответить.")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Запросы о помощи",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления о входящих запросах о помощи"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
} 