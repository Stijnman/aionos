package com.aionos.llm

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * LAN-based LLM via Ollama HTTP API.
 * Uses OpenAI-compatible /v1/chat/completions endpoint (supported since Ollama 0.1.24+).
 * Screen data never leaves the LAN.
 */
class OllamaBridge(
    private val host: String = "http://192.168.1.1:11434",
    private val model: String = "llama3.2",
    private val timeoutMs: Long = 60000
) : LLMBridge {

    override val displayName: String = "Ollama (LAN)"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = 10000
            socketTimeoutMillis = 30000
        }
        engine { threadsCount = 2 }
    }

    @Serializable
    data class ChatMessage(val role: String, val content: String)

    @Serializable
    data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val stream: Boolean = false,
        val temperature: Double = 0.8,
        val top_k: Int = 40,
        val num_ctx: Int = 4096
    )

    @Serializable
    data class ChatChoice(val message: ChatMessage)

    @Serializable
    data class ChatResponse(val choices: List<ChatChoice>)

    @Serializable
    data class TagsResponse(val models: List<ModelInfo> = emptyList())

    @Serializable
    data class ModelInfo(val name: String, val model: String? = null)

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val request = ChatRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = buildSystemPrompt()),
                ChatMessage(role = "user", content = prompt)
            )
        )
        try {
            val response: ChatResponse = withTimeoutOrNull(timeoutMs) {
                client.post("$host/v1/chat/completions") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
            } ?: throw LLMException.GenerationFailed(TimeoutCancellationException("Request timed out after $timeoutMs ms"))
            
            response.choices.firstOrNull()?.message?.content
                ?: throw LLMException.InvalidResponse(IllegalStateException("Empty response"))
        } catch (e: TimeoutCancellationException) {
            throw LLMException.GenerationFailed(e)
        } catch (e: Exception) {
            if (e is LLMException) throw e
            throw LLMException.ServerUnreachable(host)
        }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response: TagsResponse = client.get("$host/api/tags").body()
            response.models.any { it.name == model || it.model == model }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response: TagsResponse = client.get("$host/api/tags").body()
            response.models.map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun close() {
        client.close()
    }
}
