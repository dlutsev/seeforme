package com.example.seeforme

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

class CallHistoryActivity : AppCompatActivity() {
    
    private lateinit var callHistoryService: CallHistoryService
    private lateinit var callHistoryAdapter: CallHistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyHistoryTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_history)
        
        callHistoryService = CallHistoryService(this)

        recyclerView = findViewById(R.id.recycler_call_history)
        emptyHistoryTextView = findViewById(R.id.tv_empty_history)
        
        val backButton: ImageButton = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadCallHistory()
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        callHistoryAdapter = CallHistoryAdapter(emptyList())
        recyclerView.adapter = callHistoryAdapter
    }
    
    private fun loadCallHistory() {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isVolunteer = sharedPreferences.getBoolean("isVolunteer", false)
        Log.d("CallHistoryActivity", "Загрузка истории звонков для ${if (isVolunteer) "волонтера" else "пользователя"}")
        val callHistory = callHistoryService.getCallHistory(isVolunteer)
        if (callHistory.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyHistoryTextView.visibility = View.VISIBLE
            Log.d("CallHistoryActivity", "История звонков пуста")
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyHistoryTextView.visibility = View.GONE
            callHistoryAdapter.updateData(callHistory)
            Log.d("CallHistoryActivity", "Загружено ${callHistory.size} записей истории звонков")
        }
    }
} 