package com.example.seeforme

import SignalingClient
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UserMainActivity : AppCompatActivity(), SignalingClient.SignalingListener {
    
    private lateinit var signalingClient: SignalingClient
    private lateinit var statusText: TextView
    private lateinit var startCallButton: Button
    private lateinit var cancelCallButton: Button
    private lateinit var progressBar: ProgressBar
    private var inQueue = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)

        // Получаем имя пользователя из SharedPreferences
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "user") ?: "user"
        
        // Инициализируем интерфейс
        statusText = findViewById(R.id.text_status)
        startCallButton = findViewById(R.id.btn_start_call)
        cancelCallButton = findViewById(R.id.btn_cancel_call)
        progressBar = findViewById(R.id.progress_bar)
        
        // Подключаемся к серверу сигналов
        signalingClient = SignalingClient(
            "wss://seeforme.ru/v1/signal", 
            username, 
            "blind", 
            this
        )
        
        // Устанавливаем обработчики кнопок
        startCallButton.setOnClickListener {
            if (!inQueue) {
                // Запрашиваем звонок
                signalingClient.requestCall()
                inQueue = true
                updateUIForQueue(true)
                statusText.text = "Ищем доступного волонтера..."
            }
        }
        
        cancelCallButton.setOnClickListener {
            if (inQueue) {
                // TODO: Добавить отмену запроса на сервере
                inQueue = false
                updateUIForQueue(false)
                statusText.text = "Запрос отменен"
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
        
        // Начальное состояние UI
        updateUIForQueue(false)
        statusText.text = "Нажмите кнопку чтобы запросить помощь"
        
        // Подключаемся к серверу сигналов
        signalingClient.connect()
    }
    
    override fun onResume() {
        super.onResume()
        // При возвращении к активности восстанавливаем соединение, если необходимо
        if (!::signalingClient.isInitialized) {
            val username = getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("username", "user") ?: "user"
            signalingClient = SignalingClient(
                "wss://seeforme.ru/v1/signal", 
                username, 
                "blind", 
                this
            )
            signalingClient.connect()
        }
        
        // Если был в очереди, очищаем состояние
        if (inQueue) {
            inQueue = false
            updateUIForQueue(false)
            statusText.text = "Нажмите кнопку чтобы запросить помощь"
        }
    }
    
    private fun updateUIForQueue(queuing: Boolean) {
        if (queuing) {
            startCallButton.visibility = View.GONE
            cancelCallButton.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
        } else {
            startCallButton.visibility = View.VISIBLE
            cancelCallButton.visibility = View.GONE
            progressBar.visibility = View.GONE
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
        // Нашелся волонтер для звонка
        runOnUiThread {
            Toast.makeText(this, "Найден волонтер, начинаем звонок", Toast.LENGTH_SHORT).show()
            inQueue = false
            
            // Переходим на экран звонка
            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra("currentUser", signalingClient.getUserRole())
            intent.putExtra("targetUser", targetUser)
            startActivity(intent)
        }
    }
    
    override fun onCallRequest(fromUser: String) {
        // Не используется для слепого пользователя
    }
    
    override fun onCallEnded(reason: String) {
        // Если вызов завершен, обновляем UI
        runOnUiThread {
            Toast.makeText(this, "Вызов завершен: $reason", Toast.LENGTH_SHORT).show()
            inQueue = false
            updateUIForQueue(false)
            statusText.text = "Нажмите кнопку чтобы запросить помощь"
        }
    }
    
    override fun onQueueUpdate(position: Int) {
        // Обновление позиции в очереди
        runOnUiThread {
            statusText.text = "Позиция в очереди: $position"
        }
    }
}
