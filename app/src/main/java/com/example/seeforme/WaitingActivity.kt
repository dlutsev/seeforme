package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.example.seeforme.SignalingClient

class WaitingActivity : AppCompatActivity() {
    
    private var signalingClient: SignalingClient? = null
    private var userId: String = "user1"  // Значение по умолчанию
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)
        
        // Получаем ID пользователя из интента
        userId = intent.getStringExtra("userId") ?: "user1"
        
        Log.d("WaitingActivity", "Ожидание для пользователя: $userId")
        
        // Настраиваем подключение к сигнальному серверу
        setupSignalingClient(userId)
        
        findViewById<Button>(R.id.btn_cancel_request).setOnClickListener {
            cancelRequest()
        }
    }
    
    private fun setupSignalingClient(userId: String) {
        signalingClient = SignalingClient("wss://seeforme.ru/signal", userId, object : SignalingClient.SignalingListener {
            override fun onOfferReceived(offer: String) {
                // Получили предложение от волонтера, принимаем его
                Log.d("WaitingActivity", "Received offer from volunteer")
            }
            
            override fun onAnswerReceived(answer: String) {
                // Не используется для этого экрана
            }
            
            override fun onIceCandidateReceived(candidate: String) {
                // Не используется для этого экрана
            }
        })
        
        // Настраиваем обработчик события принятия запроса
        signalingClient?.setOnHelpAcceptedListener { volunteerName ->
            runOnUiThread {
                val statusText = findViewById<TextView>(R.id.tv_waiting_message)
                statusText.text = "Волонтер $volunteerName принял ваш запрос. Подготовка к соединению..."
                
                // Переходим к звонку
                startCallActivity(userId, volunteerName)
            }
        }
        
        signalingClient?.connect()
    }
    
    private fun startCallActivity(userId: String, volunteerName: String) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("currentUser", userId)
            putExtra("targetUser", volunteerName)
        }
        startActivity(intent)
        finish()
    }
    
    private fun cancelRequest() {
        // Отключаемся от сигнального сервера
        signalingClient?.disconnect()
        
        Log.d("WaitingActivity", "Запрос на помощь отменен пользователем $userId")
        
        finish()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        signalingClient?.disconnect()
    }
} 