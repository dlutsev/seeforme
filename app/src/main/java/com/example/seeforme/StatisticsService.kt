package com.example.seeforme

import android.content.Context
import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class StatisticsService {
    private val client = OkHttpClient()
    private val serverUrl = "https://seeforme.ru/statistics"
    
    fun getStatistics(callback: (StatisticsModel?) -> Unit) {
        val request = Request.Builder()
            .url(serverUrl)
            .get()
            .build()
            
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("StatisticsService", "Ошибка при получении статистики: ${e.message}")
                callback(null)
            }
            
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    try {
                        val responseBody = response.body?.string()
                        val jsonObject = JSONObject(responseBody)
                        val statistics = StatisticsModel(
                            blindCount = jsonObject.getInt("blindCount"),
                            volunteersCount = jsonObject.getInt("volunteersCount")
                        )
                        callback(statistics)
                    } catch (e: Exception) {
                        Log.e("StatisticsService", "Ошибка обработки ответа: ${e.message}")
                        callback(null)
                    }
                } else {
                    Log.e("StatisticsService", "Ошибка ответа сервера: ${response.code}")
                    callback(null)
                }
            }
        })
    }
} 