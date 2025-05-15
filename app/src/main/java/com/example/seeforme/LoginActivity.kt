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
import android.util.Log

class LoginActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.emailInput)
        etPassword = findViewById(R.id.passwordInput)
        btnLogin = findViewById(R.id.loginButton)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        val client = OkHttpClient()

        // Создаем тело запроса в формате form-urlencoded
        val formBody = FormBody.Builder()
            .add("email", email)
            .add("password", password)
            .build()

        val request = Request.Builder()
            .url("https://seeforme.ru/login")
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Ошибка авторизации: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LOGIN", "Ошибка: ${e.message}")
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                Log.d("LOGIN", "Статус: ${response.code}, Ответ: $responseBody")

                runOnUiThread {
                    if (response.isSuccessful) {
                        try {
                            val jsonResponse = JSONObject(responseBody)
                            val token = jsonResponse.getString("token")
                            val isVolunteer = responseBody?.contains("\"role\":false") == true

                            val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                            with(sharedPreferences.edit()) {
                                putBoolean("isLoggedIn", true)
                                putBoolean("isVolunteer", isVolunteer)
                                putString("token", token)
                                putString("username", email.split("@")[0]) // Сохраняем имя пользователя
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
                        } catch (e: Exception) {
                            Log.e("LOGIN", "Ошибка обработки JSON: ${e.message}")
                            Toast.makeText(applicationContext, "Ошибка обработки ответа", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d("LOGIN", "Ошибка: ${response.code}, ${response.message}")
                        Toast.makeText(applicationContext, "Ошибка авторизации: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
