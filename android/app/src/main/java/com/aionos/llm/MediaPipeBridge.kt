package com.aionos.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device LLM via MediaPipe LLM Inference API (EXPERIMENTAL).
 * Uses TensorFlow Lite Flatbuffer format (.task or .bin), NOT GGUF.
 *
 * Supported models (as of 2024): Gemma, Phi-2, Falcon, Stable LM
 * For production Android 14+ apps, consider Android AICore (Gemini Nano) instead.
 *
 * Requires: com.google.mediapipe:tasks-genai dependency
 */
class MediaPipeBridge(
    private val context: Context,
    private val modelPath: String = "${context.filesDir}/models/gemma-2b-it-gpu-int4.bin"
) : LLMBridge {

    override val displayName: String = "MediaPipe (On-Device)"

    private var llmInference: LlmInference? = null
    private var isInitialized = false

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                return@withContext Result.failure(LLMException.ModelNotFound(modelPath))
            }

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .setTopK(40)
                .setTemperature(0.8f)
                .setRandomSeed(0)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(LLMException.GenerationFailed(e))
        }
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.Default) {
        if (!isInitialized) initialize().getOrThrow()
        val inference = llmInference
            ?: throw LLMException.GenerationFailed(IllegalStateException("MediaPipe not initialized"))
        try {
            inference.generateResponse(prompt)
        } catch (e: Exception) {
            throw LLMException.GenerationFailed(e)
        }
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        File(modelPath).exists()
    }

    fun close() {
        llmInference?.close()
        llmInference = null
        isInitialized = false
    }
}
