package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        if (isLoggedIn) {
            val isVolunteer = sharedPreferences.getBoolean("isVolunteer", false)
            if (isVolunteer) {
                val intent = Intent(this, VolunteerMainActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, UserMainActivity::class.java)
                startActivity(intent)
            }
        } else {
            val intent = Intent(this, RoleActivity::class.java)
            startActivity(intent)
        }
        finish()
    }
}
