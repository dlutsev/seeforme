package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log

class VolunteerMainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_main)
        
        // Фиксированное имя волонтера для тестирования
        val volunteerId = "volunteer1"
        
        // Подписываемся на тему уведомлений для волонтеров
        subscribeToPushNotifications()
        
        // Тестовая кнопка для принятия вызова
        val startCallButton: Button = findViewById(R.id.btn_start_call)
        startCallButton.setOnClickListener {
            startCall(volunteerId, "user1")
        }
        
        // Кнопка выхода из аккаунта
        val logoutButton: Button = findViewById(R.id.btn_logout)
        logoutButton.setOnClickListener {
            // Отписываемся от пуш-уведомлений
            FirebaseMessaging.getInstance().unsubscribeFromTopic("help-request")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show()
                    }
                }
            
            // Очищаем данные сессии
            val appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            appPrefs.edit().apply {
                putBoolean("isLoggedIn", false)
                putBoolean("isVolunteer", false)
                apply()
            }
            
            // Возвращаемся на экран входа
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        
        // Проверяем, запущен ли экран из уведомления
        checkIncomingCallIntent(intent)
    }
    
    private fun subscribeToPushNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("help-request")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val statusText = findViewById<TextView>(R.id.tv_status)
                    statusText.text = "Готов к приёму запросов о помощи"
                    Toast.makeText(this, "Подписка на уведомления активирована", Toast.LENGTH_SHORT).show()
                    
                    // Получаем и показываем токен для отладки
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        Log.d("FCM_TOKEN", "Firebase token: $token")
                    }
                } else {
                    Toast.makeText(this, "Ошибка подписки на уведомления", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun startCall(volunteerId: String, targetUser: String) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("currentUser", volunteerId) 
            putExtra("targetUser", targetUser)
        }
        startActivity(intent)
    }
    
    private fun checkIncomingCallIntent(intent: Intent) {
        // Проверяем, запущено ли приложение из push-уведомления
        if (intent.hasExtra("from_notification") && intent.getBooleanExtra("from_notification", false)) {
            // Предполагаем фиксированные имена для тестирования
            Log.d("VolunteerMainActivity", "Приложение запущено из уведомления, начинаем звонок")
            startCall("volunteer1", "user1")
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Проверяем, запущен ли экран из уведомления
        checkIncomingCallIntent(intent)
    }
}
