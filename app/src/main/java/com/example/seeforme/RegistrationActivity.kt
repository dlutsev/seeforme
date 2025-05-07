package com.example.seeforme

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class RegistrationActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var btnLogin: Button
    private var isVolunteer: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registration_screen)
        isVolunteer = intent.getBooleanExtra("isVolunteer", false)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        btnRegister = findViewById(R.id.btn_register)
        btnLogin = findViewById(R.id.btn_login)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                registerUser(email, password, isVolunteer)
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser(email: String, password: String, isVolunteer: Boolean) {
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaTypeOrNull()
        val json = JSONObject()
        json.put("email", email)
        json.put("password", password)
        json.put("role", isVolunteer)
        val requestBody = RequestBody.create(mediaType, json.toString())
        val request = Request.Builder()
            .url("https://seeforme.ru/register")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Ошибка регистрации", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Ошибка регистрации", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Регистрация успешна", Toast.LENGTH_SHORT)
                            .show()
                        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            putBoolean("isLoggedIn", true)
                            putBoolean("isVolunteer", isVolunteer)
                            apply()
                        }
                        if (isVolunteer) {
                            val intent = Intent(applicationContext, VolunteerMainActivity::class.java)
                            startActivity(intent)
                        } else {
                            val intent = Intent(applicationContext, UserMainActivity::class.java)
                            startActivity(intent)
                        }
                        finish()
                    }
                }
            }
        })
    }
}
