package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class UserMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_main)

        val startCallButton: Button = findViewById(R.id.btn_start_call)
        startCallButton.setOnClickListener {
            val intent = Intent(this, CallActivity::class.java)
            intent.putExtra("currentUser", "user1")
            intent.putExtra("targetUser", "volunteer1")
            startActivity(intent)
        }
        val logoutButton: Button = findViewById(R.id.btn_logout)
        logoutButton.setOnClickListener {
            val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.putBoolean("isVolunteer", false) // Сбрасываем также роль
            editor.apply()

            // Переход на SplashActivity
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }
}

