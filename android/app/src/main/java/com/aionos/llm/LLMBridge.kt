package com.aionos.llm

/**
 * Abstraction for LLM providers.
 * Priority: Ollama (LAN) > MediaPipe (on-device, experimental) > Remote (explicit opt-in)
 */
interface LLMBridge {
    suspend fun generate(prompt: String): String
    suspend fun isAvailable(): Boolean
    val displayName: String
}

sealed class LLMException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class ModelNotFound(modelPath: String) : LLMException("Model not found: $modelPath")
    class ServerUnreachable(host: String) : LLMException("Cannot reach Ollama server at $host")
    class GenerationFailed(cause: Throwable) : LLMException("Generation failed: ${cause.message}", cause)
    class InvalidResponse(cause: Throwable) : LLMException("Invalid response from LLM: ${cause.message}", cause)
}

fun buildSystemPrompt(): String = """
You are an Android UI automation agent. You control the device via Accessibility API.

RULES:
1. Respond ONLY with a JSON array of actions. No markdown, no explanations.
2. Available actions: tap, long_press, type, scroll, swipe, open_app, press_key, read_text, wait
3. For "tap" and "long_press", use the center of the target element's bounds.
4. For "type", the target field must already be focused. Use "tap" first to focus it.
5. For "open_app", use the exact package name (e.g., "com.whatsapp").
6. For "scroll", direction is "up", "down", "left", or "right".
7. For "press_key", use: BACK, HOME, RECENTS, NOTIFICATIONS, QUICK_SETTINGS, LOCK_SCREEN
8. After each action, the system verifies state changed. If stuck, retry with different approach.
9. If you don't know what to do, emit [{"action":"wait","millis":1000}].
10. NEVER type passwords. If a password field is detected, ask the user to type it manually.

OUTPUT FORMAT — strictly JSON array:
[
  {"action":"tap","x":540,"y":1200},
  {"action":"type","text":"Hello world"},
  {"action":"scroll","direction":"down"}
]
""".trimIndent()

fun buildUserPrompt(
    userIntent: String,
    uiTree: String,
    currentApp: String,
    history: List<String> = emptyList()
): String = buildString {
    appendLine("Current app: $currentApp")
    appendLine("User intent: $userIntent")
    if (history.isNotEmpty()) {
        appendLine("Previous actions (last 5):")
        history.takeLast(5).forEach { appendLine("- $it") }
    }
    appendLine()
    appendLine("UI Tree:")
    appendLine(uiTree.take(8000))
}
