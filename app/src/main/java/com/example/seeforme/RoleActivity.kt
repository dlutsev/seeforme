package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.messaging.FirebaseMessaging

class RoleActivity : AppCompatActivity() {

    private var isVolunteer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.role_screen)

        val volunteerButton: Button = findViewById(R.id.btn_offer_help)
        val userButton: Button = findViewById(R.id.btn_need_help)

        volunteerButton.setOnClickListener {
            isVolunteer = true
            navigateToRegistration()
            Log.d("FCM", "Try to subscribe")
            FirebaseMessaging.getInstance().subscribeToTopic("help-request")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FCM", "Подписка на топик успешна")
                    } else {
                        Log.e("FCM", "Ошибка подписки", task.exception)
                    }
                }
        }

        userButton.setOnClickListener {
            isVolunteer = false
            navigateToRegistration()
        }
    }

    private fun navigateToRegistration() {
        val intent = Intent(this, RegistrationActivity::class.java)
        intent.putExtra("isVolunteer", isVolunteer)
        startActivity(intent)
    }
}
