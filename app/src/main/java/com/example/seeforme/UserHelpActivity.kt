package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import com.example.seeforme.SignalingClient

class UserHelpActivity : AppCompatActivity() {
    
    private lateinit var helpRequestService: HelpRequestService
    private var signalingClient: SignalingClient? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_help)
        
        helpRequestService = HelpRequestService(this)
        
        val questionEditText = findViewById<EditText>(R.id.et_help_question)
        val helpButton = findViewById<Button>(R.id.btn_request_help)
        
        helpButton.setOnClickListener {
            val question = questionEditText.text.toString().trim()
            
            if (question.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите ваш вопрос", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            requestHelp(question)
        }
    }
    
    private fun requestHelp(question: String) {
        // Получаем ID пользователя из SharedPreferences
        val sharedPreferences = getSharedPreferences("SeeForMePrefs", MODE_PRIVATE)
        val userId = sharedPreferences.getString("userId", "") ?: ""
        
        if (userId.isEmpty()) {
            Toast.makeText(this, "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Отправляем запрос на внешний сервис уведомлений
        helpRequestService.requestHelp(question) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Запрос о помощи отправлен", Toast.LENGTH_SHORT).show()
                    
                    // Также отправляем запрос на сигнальный сервер
                    setupSignalingAndSendRequest(userId, question)

                } else {
                    Toast.makeText(this, "Ошибка при отправке запроса", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun setupSignalingAndSendRequest(userId: String, question: String) {
        signalingClient = SignalingClient("wss://seeforme.ru/signal", userId, object : SignalingClient.SignalingListener {
            override fun onOfferReceived(offer: String) {
                // Не нужно обрабатывать оффер на этом этапе
            }
            
            override fun onAnswerReceived(answer: String) {
                // Не нужно обрабатывать ответ на этом этапе
            }
            
            override fun onIceCandidateReceived(candidate: String) {
                // Не нужно обрабатывать кандидатов на этом этапе
            }
        })
        
        signalingClient?.setOnLoginCompleteListener {
            // После успешного логина отправляем запрос о помощи на сигнальный сервер
            signalingClient?.sendHelpRequest(question)
            Log.d("UserHelpActivity", "Help request sent to signaling server")
        }
        
        signalingClient?.connect()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        signalingClient?.disconnect()
    }
} 