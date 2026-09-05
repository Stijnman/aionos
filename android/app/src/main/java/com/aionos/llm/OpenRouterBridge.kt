package com.aionos.llm

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Explicit opt-in remote provider. Screen data should only be passed by a caller that has user consent. */
class OpenRouterBridge(
    private val apiKey: String,
    private val model: String = "openai/gpt-4o-mini",
    private val endpoint: String = "https://openrouter.ai/api/v1/chat/completions"
) : LLMBridge {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    override val displayName: String = "OpenRouter (remote, opt-in)"

    override suspend fun isAvailable(): Boolean = apiKey.isNotBlank() && endpoint.startsWith("https://")

    override suspend fun generate(prompt: String): String {
        require(isAvailable()) { "OpenRouter is not configured or is not HTTPS" }
        val response = client.post(endpoint) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("HTTP-Referer", "https://github.com/Stijnman/aionos")
            header("X-Title", "AionOS")
            setBody(ChatRequest(model, listOf(Message("user", prompt))))
        }
        return response.body<ChatResponse>().choices.firstOrNull()?.message?.content
            ?: throw LLMException.InvalidResponse(IllegalStateException("No content in OpenRouter response"))
    }

    fun close() { client.close() }

    @Serializable
    private data class ChatRequest(val model: String, val messages: List<Message>)

    @Serializable
    private data class Message(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: Message)
}
