package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.example.seeforme.SignalingClient

class IncomingCallActivity : AppCompatActivity() {
    
    private var userId: String? = null
    private var question: String? = null
    private var requestId: String? = null
    private var signalingClient: SignalingClient? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)
        
        // Получаем данные из пуш-уведомления или интента
        userId = intent.getStringExtra("userId") 
        question = intent.getStringExtra("question") ?: "Запрос помощи"
        requestId = intent.getStringExtra("requestId") // может быть null из пуш-уведомления
        
        if (userId == null) {
            Log.e("IncomingCallActivity", "userId не получен, невозможно принять звонок")
            finish()
            return
        }
        
        // Устанавливаем информацию о звонке
        val userIdTextView = findViewById<TextView>(R.id.tv_user_id)
        val questionTextView = findViewById<TextView>(R.id.tv_question)
        
        userIdTextView.text = "ID пользователя: $userId"
        questionTextView.text = "Вопрос: $question"
        
        // Настраиваем кнопки принятия и отклонения звонка
        val acceptButton = findViewById<Button>(R.id.btn_accept_call)
        val declineButton = findViewById<Button>(R.id.btn_decline_call)
        
        acceptButton.setOnClickListener {
            acceptCall()
        }
        
        declineButton.setOnClickListener {
            declineCall()
        }
        
        // Подключаемся к сигнальному серверу
        setupSignalingClient()
    }
    
    private fun setupSignalingClient() {
        // Получаем ID волонтера
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val volunteerId = sharedPreferences.getString("username", "volunteer") ?: "volunteer"
        
        Log.d("IncomingCallActivity", "Волонтер $volunteerId принимает звонок от $userId")
        
        signalingClient = SignalingClient("wss://seeforme.ru/signal", volunteerId, object : SignalingClient.SignalingListener {
            override fun onOfferReceived(offer: String) {
                // Не требуется для этого экрана
            }
            
            override fun onAnswerReceived(answer: String) {
                // Не требуется для этого экрана
            }
            
            override fun onIceCandidateReceived(candidate: String) {
                // Не требуется для этого экрана
            }
        })
        
        // Когда соединение с сервером установлено
        signalingClient?.setOnLoginCompleteListener {
            // Если у нас есть ID запроса, принимаем его автоматически
            if (requestId != null) {
                runOnUiThread {
                    signalingClient?.acceptHelpRequest(requestId!!)
                }
            }
        }
        
        // Когда установлено соединение с пользователем
        signalingClient?.setOnHelpAcceptedListener { userId ->
            runOnUiThread {
                startCall()
            }
        }
        
        signalingClient?.connect()
    }
    
    private fun acceptCall() {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val volunteerId = sharedPreferences.getString("username", "volunteer") ?: "volunteer"
        
        if (requestId != null) {
            // Если есть ID запроса, принимаем его через сигнальный сервер
            signalingClient?.acceptHelpRequest(requestId!!)
        }
        
        // В любом случае переходим к звонку
        startCall()
    }
    
    private fun startCall() {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val volunteerId = sharedPreferences.getString("username", "volunteer") ?: "volunteer"
        
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("currentUser", volunteerId)
            putExtra("targetUser", userId)
        }
        startActivity(intent)
        finish()
    }
    
    private fun declineCall() {
        // Сообщаем пользователю, что волонтер отклонил запрос (если нужно)
        // Здесь можно добавить отправку сообщения на сервер
        
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        signalingClient?.disconnect()
    }
} 