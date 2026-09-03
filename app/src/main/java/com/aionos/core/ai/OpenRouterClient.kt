package com.aionos.core.ai

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

/**
 * Client for interacting with the OpenRouter API.
 * Handles authentication, request formatting, and response parsing.
 */
class OpenRouterClient(
    private val apiKey: String,
    private val baseUrl: String = "https://openrouter.ai/api/v1/chat/completions"
) {
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json".toMediaType()

    /**
     * Generates a response from OpenRouter's API for the given prompt.
     *
     * @param prompt The user prompt to send to the API.
     * @param model The model to use (default: openai/gpt-4o-mini).
     * @param temperature The sampling temperature (default: 0.7).
     * @return The generated response text.
     * @throws OpenRouterException if the API call fails.
     */
    suspend fun generate(
        prompt: String,
        model: String = "openai/gpt-4o-mini",
        temperature: Double = 0.7
    ): String {
        val requestBody = """
            {
                "model": "$model",
                "messages": [
                    {"role": "user", "content": "$prompt"}
                ],
                "temperature": $temperature
            }
        """.trimIndent()

        val request = Request.Builder()
            .url(baseUrl)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(requestBody.toRequestBody(jsonMediaType))
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw OpenRouterException("API call failed: ${response.code} - ${response.message}")
            }
            parseResponse(response)
        } catch (e: IOException) {
            throw OpenRouterException("Network error: ${e.message}")
        }
    }

    /**
     * Parses the response from OpenRouter's API.
     *
     * @param response The HTTP response from the API.
     * @return The generated response text.
     */
    private fun parseResponse(response: Response): String {
        val responseBody = response.body?.string() ?: throw OpenRouterException("Empty response")
        // Simple parsing: Extract the first choice's content
        // Note: In production, use a JSON parser like Gson or Moshi
        return responseBody
    }
}

/**
 * Exception thrown when OpenRouter API calls fail.
 */
class OpenRouterException(message: String) : Exception(message)