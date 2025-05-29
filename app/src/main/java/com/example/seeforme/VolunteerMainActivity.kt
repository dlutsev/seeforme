package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
import android.view.View

class VolunteerMainActivity : AppCompatActivity() {

    private lateinit var statisticsService: StatisticsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volunteer_main)
        statisticsService = StatisticsService()
        subscribeToPushNotifications()
        loadStatistics()
        
        val callHistoryButton: Button = findViewById(R.id.btn_call_history)
        callHistoryButton.setOnClickListener {
            openCallHistory()
        }
        
        val logoutButton: Button = findViewById(R.id.btn_logout)
        logoutButton.setOnClickListener {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("help-request")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Вы вышли из системы", Toast.LENGTH_SHORT).show()
                    }
                }
            val appPrefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            appPrefs.edit().apply {
                putBoolean("isLoggedIn", false)
                putBoolean("isVolunteer", false)
                apply()
            }
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        checkIncomingCallIntent(intent)
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
                    Log.d("VolunteerMainActivity", "Статистика загружена: ${statistics.blindCount} пользователей, ${statistics.volunteersCount} волонтеров")
                } else {
                    Toast.makeText(this, "Не удалось загрузить статистику", Toast.LENGTH_SHORT).show()
                    Log.e("VolunteerMainActivity", "Ошибка загрузки статистики")
                }
            }
        }
    }
    
    private fun subscribeToPushNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("help-request")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val statusText = findViewById<TextView>(R.id.tv_status)
                    statusText.text = "Готов к приёму запросов о помощи"
                    Toast.makeText(this, "Подписка на уведомления активирована", Toast.LENGTH_SHORT).show()
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        Log.d("FCM_TOKEN", "Firebase token: $token")
                    }
                } else {
                    Toast.makeText(this, "Ошибка подписки на уведомления", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    private fun startCall(volunteerId: String, targetUser: String) {
        Log.d("VolunteerMainActivity", "Начинаем звонок. volunteerId=$volunteerId, targetUser=$targetUser")
        val intent = Intent(this, CallActivity::class.java).apply {
            putExtra("currentUser", volunteerId) 
            putExtra("targetUser", targetUser)
        }
        startActivity(intent)
    }
    
    private fun openCallHistory() {
        val intent = Intent(this, CallHistoryActivity::class.java)
        startActivity(intent)
    }
    
    private fun checkIncomingCallIntent(intent: Intent) {
        if (intent.hasExtra("from_notification") && intent.getBooleanExtra("from_notification", false)) {
            Log.d("VolunteerMainActivity", "Приложение запущено из уведомления, начинаем звонок")
            startCall("volunteer1", "user1")
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        checkIncomingCallIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
        loadStatistics()
    }
}
