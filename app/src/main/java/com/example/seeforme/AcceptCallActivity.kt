package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AcceptCallActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accept_call)
        
        // Получаем данные из уведомления
        val callerName = intent.getStringExtra("caller_name") ?: "Неизвестный пользователь"
        
        // Отображаем информацию о звонящем
        val callerInfoText = findViewById<TextView>(R.id.text_caller_info)
        callerInfoText.text = "Входящий вызов от: $callerName"
        
        // Кнопка принятия вызова
        val acceptButton = findViewById<Button>(R.id.btn_accept_call)
        acceptButton.setOnClickListener {
            val callIntent = Intent(this, CallActivity::class.java)
            callIntent.putExtra("currentUser", getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("username", "volunteer"))
            callIntent.putExtra("targetUser", callerName)
            callIntent.putExtra("isFromNotification", true)
            startActivity(callIntent)
            finish()
        }
        
        // Кнопка отклонения вызова
        val declineButton = findViewById<Button>(R.id.btn_decline_call)
        declineButton.setOnClickListener {
            finish()
        }
    }
} 