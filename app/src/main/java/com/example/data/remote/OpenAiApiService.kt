package com.example.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class OpenAiMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Double = 0.2
)

@JsonClass(generateAdapter = true)
data class OpenAiChoice(
    val index: Int = 0,
    val message: OpenAiMessage? = null
)

@JsonClass(generateAdapter = true)
data class OpenAiChatResponse(
    val id: String = "",
    val choices: List<OpenAiChoice> = emptyList()
)

interface OpenAiApiService {
    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse
}
