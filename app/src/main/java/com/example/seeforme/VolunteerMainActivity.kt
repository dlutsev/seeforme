package com.example.seeforme

import SignalingClient
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VolunteerMainActivity : AppCompatActivity(), SignalingClient.SignalingListener {
    
    private lateinit var signalingClient: SignalingClient
    private lateinit var statusText: TextView
    private lateinit var availabilitySwitch: Switch
    private var isAvailable = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_main)
        
        // Получаем имя пользователя из SharedPreferences
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "volunteer") ?: "volunteer"
        
        // Инициализируем интерфейс
        statusText = findViewById(R.id.text_status)
        availabilitySwitch = findViewById(R.id.switch_availability)
        
        // Подключаемся к серверу сигналов
        signalingClient = SignalingClient(
            "wss://seeforme.ru/v1/signal", 
            username, 
            "volunteer", 
            this
        )
        
        // Устанавливаем обработчик переключателя доступности
        availabilitySwitch.setOnCheckedChangeListener { _, isChecked ->
            isAvailable = isChecked
            if (isChecked) {
                signalingClient.reportVolunteerReady()
                statusText.text = "Статус: Готов принимать вызовы"
            } else {
                statusText.text = "Статус: Не готов"
            }
        }
        
        // Кнопка выхода
        val logoutButton: Button = findViewById(R.id.btn_logout)
        logoutButton.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.putBoolean("isVolunteer", false)
            editor.apply()
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        
        // Подключаемся к серверу сигналов
        signalingClient.connect()
        
        // Устанавливаем начальный статус
        statusText.text = "Статус: Не готов"
    }
    
    override fun onResume() {
        super.onResume()
        // При возвращении к активности восстанавливаем соединение, если необходимо
        if (!::signalingClient.isInitialized) {
            val username = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("username", "volunteer") ?: "volunteer"
            signalingClient = SignalingClient(
                "wss://seeforme.ru/v1/signal", 
                username, 
                "volunteer", 
                this
            )
            signalingClient.connect()
        }
    }
    
    // Реализация интерфейса SignalingListener
    override fun onOfferReceived(offer: String) {
        // Не используется в этой активности
    }
    
    override fun onAnswerReceived(answer: String) {
        // Не используется в этой активности
    }
    
    override fun onIceCandidateReceived(candidate: String) {
        // Не используется в этой активности
    }
    
    override fun onCallMatched(targetUser: String) {
        // Не используется в этой активности
    }
    
    override fun onCallRequest(fromUser: String) {
        // Получен запрос на звонок
        runOnUiThread {
            // Переходим на экран принятия вызова
            val intent = Intent(this, AcceptCallActivity::class.java)
            intent.putExtra("caller_name", fromUser)
            startActivity(intent)
        }
    }
    
    override fun onCallEnded(reason: String) {
        // Если вызов завершен, обновляем статус
        runOnUiThread {
            Toast.makeText(this, "Вызов завершен: $reason", Toast.LENGTH_SHORT).show()
            
            // Автоматически становимся доступным для следующего вызова
            if (isAvailable) {
                signalingClient.reportVolunteerReady()
            }
        }
    }
    
    override fun onQueueUpdate(position: Int) {
        // Не используется для волонтера
    }
}
