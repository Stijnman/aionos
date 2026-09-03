package com.aionos.agent

import android.content.Context
import com.aionos.action.AgentAction
import com.aionos.action.SafeActionExecutor
import com.aionos.audit.AuditLog
import com.aionos.llm.*
import com.aionos.parser.ActionParser
import com.aionos.security.EncryptedPrefs
import com.aionos.service.AgentAccessibilityService
import com.aionos.voice.VoiceInputManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central orchestrator for the AionOS agent.
 * Coordinates: voice input → LLM planning → action execution → feedback loop.
 */
class AgentOrchestrator(
    private val context: Context,
    private val service: AgentAccessibilityService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val prefs = EncryptedPrefs.getInstance(context)
    private val auditLog = AuditLog(context)
    private val actionParser = ActionParser()

    private var llmBridge: LLMBridge? = null
    private var voiceManager: VoiceInputManager? = null

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private val actionHistory = mutableListOf<String>()
    
    // Mutex to prevent concurrent execution
    private val executionMutex = Mutex()

    fun initialize() {
        llmBridge = when (prefs.llmProvider) {
            "mediapipe" -> MediaPipeBridge(context)
            "ollama" -> OllamaBridge(host = prefs.ollamaHost, model = prefs.ollamaModel)
            else -> OllamaBridge(host = prefs.ollamaHost, model = prefs.ollamaModel)
        }
    }

    fun executeIntent(userIntent: String, maxSteps: Int = 10) {
        scope.launch {
            // Use mutex to prevent concurrent execution
            if (!executionMutex.tryLock()) {
                _transcript.value = "Agent is busy. Please wait."
                return@launch
            }
            
            executionMutex.withLock {
                _state.value = AgentState.Running(userIntent)
                _transcript.value = ""

                try {
                    var steps = 0
                    var completed = false

                    while (steps < maxSteps && !completed) {
                        steps++

                        val currentApp = service.rootInActiveWindow?.packageName?.toString() ?: "unknown"
                        val uiTree = service.getCurrentTree()

                        val systemPrompt = buildSystemPrompt()
                        val userPrompt = buildUserPrompt(userIntent, uiTree, currentApp, actionHistory)
                        val fullPrompt = "$systemPrompt\n\n$userPrompt"

                        val bridge = llmBridge ?: throw IllegalStateException("LLM not initialized")
                        val response = bridge.generate(fullPrompt)

                        val actions = actionParser.parse(response)

                        when (val validation = actionParser.validate(actions)) {
                            is ActionParser.ValidationResult.Invalid -> {
                                _transcript.value += "\nValidation failed: ${validation.errors.joinToString()}"
                                break
                            }
                            else -> {}
                        }

                        if (actions.isEmpty()) {
                            _transcript.value += "\nLLM returned no valid actions. Retrying..."
                            delay(1000)
                            continue
                        }

                        for (action in actions) {
                            val result = service.actionExecutor.execute(action)
                            result.onSuccess { msg ->
                                actionHistory.add("${action.javaClass.simpleName}: $msg")
                                _transcript.value += "\n✓ $msg"
                            }.onFailure { err ->
                                actionHistory.add("${action.javaClass.simpleName}: FAILED - ${err.message}")
                                _transcript.value += "\n✗ ${err.message}"
                            }
                            if (action is AgentAction.Wait && action.millis > 2000) completed = true
                        }
                        delay(500)
                    }
                    _state.value = AgentState.Completed(_transcript.value)
                } catch (e: Exception) {
                    _state.value = AgentState.Error(e.message ?: "Unknown error")
                    _transcript.value += "\nError: ${e.message}"
                }
            }
        }
    }

    fun startVoiceCommand() {
        if (voiceManager == null) {
            voiceManager = VoiceInputManager(context).apply { initialize() }
        }
        val vm = voiceManager ?: return

        scope.launch {
            vm.state.collect { state ->
                if (state is VoiceInputManager.VoiceState.Error) {
                    _transcript.value = "Voice error: ${state.message}"
                    _state.value = AgentState.Error(state.message)
                }
            }
        }
        scope.launch {
            vm.transcript.collect { text ->
                if (text.isNotBlank() && vm.state.value is VoiceInputManager.VoiceState.Idle) {
                    executeIntent(text)
                }
            }
        }
        vm.startListening()
        _state.value = AgentState.Listening
    }

    fun stopVoiceCommand() {
        voiceManager?.stopListening()
    }

    fun cancel() {
        scope.coroutineContext.cancelChildren()
        _state.value = AgentState.Idle
    }

    fun destroy() {
        cancel()
        (llmBridge as? MediaPipeBridge)?.close()
        (llmBridge as? OllamaBridge)?.close()
        voiceManager?.destroy()
        llmBridge = null
        voiceManager = null
    }

    sealed class AgentState {
        object Idle : AgentState()
        object Listening : AgentState()
        data class Running(val intent: String) : AgentState()
        data class Completed(val result: String) : AgentState()
        data class Error(val message: String) : AgentState()
    }
}
