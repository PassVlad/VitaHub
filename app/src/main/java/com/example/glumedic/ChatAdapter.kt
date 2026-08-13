package com.example.glumedic

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private var messages: List<ChatMessageResponse>
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvQuestion: TextView = itemView.findViewById(R.id.tvQuestion)
        val tvAnswer: TextView = itemView.findViewById(R.id.tvAnswer)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.tvQuestion.text = "👤 ${message.question}"
        holder.tvAnswer.text = "🤖 ${message.answer}"
        
        if (message.created_at.isNotEmpty()) {
            // Форматируем дату из "2026-06-09T12:00:00Z" в "09.06.2026 12:00"
            val formattedDate = try {
                val parts = message.created_at.replace("Z", "").split("T")
                val date = parts[0].split("-").reversed().joinToString(".")
                val time = parts[1].take(5)
                "$date $time"
            } catch (e: Exception) {
                message.created_at
            }
            holder.tvTimestamp.text = formattedDate
            holder.tvTimestamp.visibility = View.VISIBLE
        } else {
            holder.tvTimestamp.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<ChatMessageResponse>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}