package com.example.glumedic

import retrofit2.Call
import retrofit2.http.*

interface ChatAiApi {
    
    // Отправить сообщение в чат
    @POST("api/chat/")
    fun sendMessage(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): Call<ChatMessageResponse>
    
    // Получить историю чатов
    @GET("api/chat/")
    fun getChatHistory(
        @Header("Authorization") token: String
    ): Call<List<ChatMessageResponse>>
}

data class ChatRequest(
    val question: String
)

data class ChatMessageResponse(
    val id: Int,
    val question: String,
    val answer: String,
    val created_at: String
)