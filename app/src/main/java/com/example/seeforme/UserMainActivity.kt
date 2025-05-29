package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View

class UserMainActivity : AppCompatActivity() {
    
    private lateinit var helpRequestService: HelpRequestService
    private lateinit var statisticsService: StatisticsService
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)
        helpRequestService = HelpRequestService(this)
        statisticsService = StatisticsService()
        loadStatistics()
        
        val requestHelpButton: Button = findViewById(R.id.btn_request_help)
        requestHelpButton.setOnClickListener {
            sendHelpRequestAndCall()
        }
        
        val callHistoryButton: Button = findViewById(R.id.btn_call_history)
        callHistoryButton.setOnClickListener {
            openCallHistory()
        }
        
        val logoutButton: Button = findViewById(R.id.btn_logout)
        logoutButton.setOnClickListener {
            val appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val editor = appPrefs.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.putBoolean("isVolunteer", false)
            editor.apply()
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
    
    private fun loadStatistics() {
        val statisticsView = findViewById<View>(R.id.statistics_view)
        val blindCountTextView = statisticsView.findViewById<TextView>(R.id.tv_blind_count)
        val volunteersCountTextView = statisticsView.findViewById<TextView>(R.id.tv_volunteers_count)
        statisticsService.getStatistics { statistics ->
            runOnUiThread {
                if (statistics != null) {
                    blindCountTextView.text = statistics.blindCount.toString()
                    volunteersCountTextView.text = statistics.volunteersCount.toString()
                    Log.d("UserMainActivity", "Статистика загружена: ${statistics.blindCount} пользователей, ${statistics.volunteersCount} волонтеров")
                } else {
                    Toast.makeText(this, "Не удалось загрузить статистику", Toast.LENGTH_SHORT).show()
                    Log.e("UserMainActivity", "Ошибка загрузки статистики")
                }
            }
        }
    }
    
    private fun sendHelpRequestAndCall() {
        val userId = "user1"
        val targetVolunteer = "volunteer1"
        val helpMessage = "Нужна помощь слабовидящему пользователю"
        Log.d("UserMainActivity", "Отправляем запрос о помощи от $userId к $targetVolunteer")
        helpRequestService.requestHelp(helpMessage) { success ->
            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "Запрос о помощи отправлен, начинаем звонок", Toast.LENGTH_LONG).show()
                    startCall(userId, targetVolunteer)
                } else {
                    Toast.makeText(this, "Ошибка при отправке запроса о помощи", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    private fun startCall(userId: String, targetVolunteer: String) {
        Log.d("UserMainActivity", "Начинаем звонок. userId=$userId, targetVolunteer=$targetVolunteer")
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("currentUser", userId)
            putExtra("targetUser", targetVolunteer)
        }
        startActivity(intent)
    }
    
    private fun openCallHistory() {
        val intent = Intent(this, CallHistoryActivity::class.java)
        startActivity(intent)
    }
    
    override fun onResume() {
        super.onResume()
        loadStatistics()
    }
}

