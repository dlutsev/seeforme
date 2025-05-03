package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

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
