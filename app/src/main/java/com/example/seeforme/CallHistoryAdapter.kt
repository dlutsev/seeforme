package com.example.seeforme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class CallHistoryAdapter(private var callHistory: List<CallHistoryItem>) :
    RecyclerView.Adapter<CallHistoryAdapter.ViewHolder>() {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val contactNameTextView: TextView = view.findViewById(R.id.tv_contact_name)
        val callDateTextView: TextView = view.findViewById(R.id.tv_call_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val callItem = callHistory[position]
        holder.contactNameTextView.text = "${callItem.contactName} ${callItem.contactId}"
        holder.callDateTextView.text = dateFormat.format(callItem.callDate)
    }

    override fun getItemCount() = callHistory.size

    fun updateData(newCallHistory: List<CallHistoryItem>) {
        callHistory = newCallHistory
        notifyDataSetChanged()
    }
} 