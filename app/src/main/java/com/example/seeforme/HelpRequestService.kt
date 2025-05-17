package com.example.seeforme

import android.content.Context
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class HelpRequestService(private val context: Context) {
    
    private val client = OkHttpClient()
    private val helpEndpoint = "https://seeforme.ru/help"
    
    fun requestHelp(question: String, callback: (Boolean) -> Unit) {
        Log.d("HelpRequestService", "Отправка простого запроса о помощи: $question")
        
        val jsonBody = JSONObject().apply {
            put("question", question)
        }
        
        val requestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            jsonBody.toString()
        )
        
        val request = Request.Builder()
            .url(helpEndpoint)
            .post(requestBody)
            .header("Content-Type", "application/json")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("HelpRequestService", "Ошибка отправки запроса: ${e.message}")
                callback(false)
            }
            
            override fun onResponse(call: Call, response: Response) {
                val success = response.isSuccessful
                val responseBody = response.body?.string()
                
                if (success) {
                    Log.d("HelpRequestService", "Запрос успешно отправлен! Ответ: $responseBody")
                } else {
                    Log.e("HelpRequestService", "Сервер вернул ошибку: ${response.code}, $responseBody")
                }
                callback(success)
            }
        })
    }
} 