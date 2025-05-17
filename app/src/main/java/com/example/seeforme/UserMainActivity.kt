package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class UserMainActivity : AppCompatActivity() {
    
    private lateinit var helpRequestService: HelpRequestService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)

        // Инициализируем сервис запросов помощи
        helpRequestService = HelpRequestService(this)
        
        // Кнопка запроса помощи
        val requestHelpButton: Button = findViewById(R.id.btn_request_help)
        requestHelpButton.setOnClickListener {
            // Отправляем запрос о помощи и запускаем звонок
            sendHelpRequestAndCall()
        }
        
        // Кнопка выхода из аккаунта
        val logoutButton: Button = findViewById(R.id.btn_logout)
        logoutButton.setOnClickListener {
            val appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val editor = appPrefs.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.putBoolean("isVolunteer", false)
            editor.apply()

            // Переходим на экран входа
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
    
    private fun sendHelpRequestAndCall() {
        // Фиксированные имена для тестирования
        val userId = "user1"
        val targetVolunteer = "volunteer1"
        val helpMessage = "Нужна помощь слабовидящему пользователю"
        
        Log.d("UserMainActivity", "Отправляем запрос о помощи от $userId к $targetVolunteer")
        
        // Отправляем запрос на сервер уведомлений
        helpRequestService.requestHelp(helpMessage) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Запрос о помощи отправлен, начинаем звонок", Toast.LENGTH_LONG).show()
                    
                    // Сразу начинаем звонок
                    startCall(userId, targetVolunteer)
                } else {
                    Toast.makeText(this, "Ошибка при отправке запроса о помощи", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun startCall(userId: String, targetVolunteer: String) {
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("currentUser", userId)
            putExtra("targetUser", targetVolunteer)
        }
        startActivity(intent)
    }
}

