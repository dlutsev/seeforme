package com.example.seeforme

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class CallHistoryService(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val TAG = "CallHistoryService"
    private fun getSharedPreferences(isVolunteer: Boolean): SharedPreferences {
        val prefName = if (isVolunteer) "VolunteerCallHistoryPrefs" else "UserCallHistoryPrefs"
        Log.d(TAG, "Используем SharedPreferences: $prefName для ${if (isVolunteer) "волонтера" else "пользователя"}")
        return context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    }
    
    fun saveCall(contactId: String, contactName: String, isVolunteer: Boolean) {
        try {
            val sharedPreferences = getSharedPreferences(isVolunteer)
            val callDate = Date()
            val callHistoryJson = sharedPreferences.getString("callHistory", "[]")
            Log.d(TAG, "Текущая история звонков: $callHistoryJson")
            val callHistoryArray = JSONArray(callHistoryJson)
            val callItem = JSONObject().apply {
                put("callDate", dateFormat.format(callDate))
                put("contactId", contactId)
                put("contactName", contactName)
            }
            
            Log.d(TAG, "Добавляем запись в историю: $callItem")
            callHistoryArray.put(callItem)
            val newCallHistoryJson = callHistoryArray.toString()
            Log.d(TAG, "Новая история звонков: $newCallHistoryJson")
            val success = sharedPreferences.edit()
                .putString("callHistory", newCallHistoryJson)
                .commit()
            
            if (success) {
                Log.d(TAG, "Изменения успешно сохранены в SharedPreferences (commit=true)")
            } else {
                Log.e(TAG, "Не удалось сохранить изменения в SharedPreferences (commit=false)")
            }
            val verifyJson = sharedPreferences.getString("callHistory", "[]")
            Log.d(TAG, "Проверка сохранения: $verifyJson")
            
            Log.d(TAG, "Звонок сохранен для ${if (isVolunteer) "волонтера" else "пользователя"}: $contactName ($contactId) в ${dateFormat.format(callDate)}")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при сохранении звонка: ${e.message}", e)
        }
    }
    
    fun getCallHistory(isVolunteer: Boolean): List<CallHistoryItem> {
        val callHistoryList = mutableListOf<CallHistoryItem>()
        try {
            val sharedPreferences = getSharedPreferences(isVolunteer)
            val callHistoryJson = sharedPreferences.getString("callHistory", "[]")
            Log.d(TAG, "Получена история звонков из SharedPreferences: $callHistoryJson")
            
            val callHistoryArray = JSONArray(callHistoryJson)
            Log.d(TAG, "Количество записей в истории: ${callHistoryArray.length()}")
            
            for (i in 0 until callHistoryArray.length()) {
                val callItem = callHistoryArray.getJSONObject(i)
                val callDateStr = callItem.getString("callDate")
                val callDate = dateFormat.parse(callDateStr)
                val contactId = callItem.getString("contactId")
                val contactName = callItem.getString("contactName")
                
                Log.d(TAG, "Обработка записи #$i: дата=$callDateStr, contactId=$contactId, contactName=$contactName")
                
                if (callDate != null) {
                    callHistoryList.add(CallHistoryItem(callDate, contactId, contactName))
                    Log.d(TAG, "Запись #$i добавлена в список")
                } else {
                    Log.e(TAG, "Не удалось распарсить дату: $callDateStr")
                }
            }
            callHistoryList.sortByDescending { it.callDate }
            
            Log.d(TAG, "Получена история звонков для ${if (isVolunteer) "волонтера" else "пользователя"}: ${callHistoryList.size} записей")
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при получении истории звонков: ${e.message}", e)
        }
        
        return callHistoryList
    }

} 