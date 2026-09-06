package com.aionos.agent

import android.content.Context
import com.aionos.action.AgentAction
import com.aionos.audit.AuditLog
import com.aionos.llm.LLMBridge
import com.aionos.llm.MediaPipeBridge
import com.aionos.llm.OllamaBridge
import com.aionos.llm.OpenRouterBridge
import com.aionos.parser.ActionParser
import com.aionos.security.EncryptedPrefs
import com.aionos.service.AgentAccessibilityService
import com.aionos.voice.VoiceInputManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Coordinates voice/text input, model planning, policy-checked execution, and feedback. */
class AgentOrchestrator(
    private val context: Context,
    private val service: AgentAccessibilityService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs = EncryptedPrefs.getInstance(context)
    private val actionParser = ActionParser()

    private var llmBridge: LLMBridge? = null
    private var voiceManager: VoiceInputManager? = null
    private var voiceStateJob: Job? = null

    private val _state = MutableStateFlow<AgentState>(AgentState.Idle)
    val state: StateFlow<AgentState> = _state
    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript
    private val actionHistory = ArrayDeque<String>(20)

    fun initialize() {
        closeBridge()
        llmBridge = when (prefs.llmProvider) {
            "mediapipe" -> MediaPipeBridge(context)
            "openrouter" -> {
                check(prefs.remoteProviderConsent) { "Remote provider consent is required" }
                OpenRouterBridge(prefs.openRouterApiKey, prefs.openRouterModel)
            }
            else -> OllamaBridge(prefs.ollamaHost, prefs.ollamaModel)
        }
    }

    fun executeIntent(userIntent: String, maxSteps: Int = 10) {
        val intent = userIntent.trim()
        if (intent.isEmpty() || _state.value is AgentState.Running) return
        scope.launch {
            _state.value = AgentState.Running(intent)
            _transcript.value = ""
            try {
                val bridge = llmBridge ?: error("LLM is not initialized")
                var completed = false
                var steps = 0
                while (!completed && steps < maxSteps) {
                    ensureActive()
                    steps++
                    val currentApp = service.rootInActiveWindow?.packageName?.toString() ?: "unknown"
                    val response = bridge.generate(
                        "${buildSystemPrompt()}\n\n${buildUserPrompt(intent, service.getCurrentTree(), currentApp, actionHistory.toList())}"
                    )
                    val actions = actionParser.parse(response)
                    when (val validation = actionParser.validate(actions)) {
                        is ActionParser.ValidationResult.Invalid -> {
                            val message = "Action plan rejected: ${validation.errors.joinToString()}"
                            _transcript.value = message
                            _state.value = AgentState.Error(message)
                            return@launch
                        }
                        else -> Unit
                    }
                    if (actions.isEmpty()) {
                        val message = "The model returned no executable actions"
                        _transcript.value = message
                        _state.value = AgentState.Error(message)
                        return@launch
                    }

                    var failed: Throwable? = null
                    for (action in actions) {
                        ensureActive()
                        val result = service.actionExecutor.execute(action)
                        if (result.isSuccess) {
                            actionHistory.addLast("${action.javaClass.simpleName}:ok")
                            _transcript.value += "\n✓ ${safeResultMessage(action)}"
                        } else {
                            failed = result.exceptionOrNull() ?: IllegalStateException("Action failed")
                            actionHistory.addLast("${action.javaClass.simpleName}:failed")
                            _transcript.value += "\n✗ Action failed: ${failed.message ?: "unknown error"}"
                            break
                        }
                    }
                    while (actionHistory.size > 20) actionHistory.removeFirst()
                    if (failed != null) {
                        _state.value = AgentState.Error("Execution stopped after an action failure")
                        return@launch
                    }
                    // One fully successful, policy-checked plan is a completed command. A later
                    // verification/replan can be added without misusing a wait as proof of success.
                    completed = true
                }
                if (!completed) _state.value = AgentState.Error("Maximum planning steps reached")
                else _state.value = AgentState.Completed(_transcript.value)
            } catch (cancelled: CancellationException) {
                _state.value = AgentState.Idle
                throw cancelled
            } catch (e: Exception) {
                _state.value = AgentState.Error(e.message ?: "Unknown error")
                _transcript.value += "\nError: ${e.message ?: "Unknown error"}"
            }
        }
    }

    fun startVoiceCommand() {
        val vm = voiceManager ?: VoiceInputManager(context).also { voiceManager = it }
        if (voiceStateJob?.isActive == true) return
        val initialized = vm.initialize()
        if (initialized.isFailure) {
            val message = initialized.exceptionOrNull()?.message ?: "Voice model is unavailable"
            _state.value = AgentState.Error(message)
            _transcript.value = "Voice error: $message"
            return
        }
        voiceStateJob = scope.launch {
            vm.state.collect { state ->
                when (state) {
                    is VoiceInputManager.VoiceState.Error -> {
                        _state.value = AgentState.Error(state.message)
                        _transcript.value = "Voice error: ${state.message}"
                    }
                    is VoiceInputManager.VoiceState.Listening -> _state.value = AgentState.Listening
                    is VoiceInputManager.VoiceState.Idle -> {
                        val command = vm.transcript.value.trim()
                        if (command.isNotEmpty() && _state.value !is AgentState.Running) {
                            vm.clearTranscript()
                            executeIntent(command)
                        }
                    }
                    else -> Unit
                }
            }
        }
        vm.startListening()
        _state.value = AgentState.Listening
    }

    fun stopVoiceCommand() {
        voiceManager?.stopListening()
        voiceStateJob?.cancel()
        voiceStateJob = null
        if (_state.value is AgentState.Listening) _state.value = AgentState.Idle
    }

    fun cancel() {
        scope.coroutineContext.cancelChildren()
        voiceManager?.stopListening()
        _state.value = AgentState.Idle
    }

    fun destroy() {
        stopVoiceCommand()
        cancel()
        voiceManager?.destroy()
        voiceManager = null
        closeBridge()
        scope.cancel()
    }

    private fun closeBridge() {
        when (val bridge = llmBridge) {
            is MediaPipeBridge -> bridge.close()
            is OllamaBridge -> bridge.close()
            is OpenRouterBridge -> bridge.close()
        }
        llmBridge = null
    }

    private fun safeResultMessage(action: AgentAction): String = when (action) {
        is AgentAction.Type -> "Text input completed (${action.text.length} characters)"
        else -> "${action.javaClass.simpleName} completed"
    }

    private fun ensureActive() {
        check(prefs.isAgentEnabled) { "Agent is paused by the kill switch" }
    }

    sealed class AgentState {
        data object Idle : AgentState()
        data object Listening : AgentState()
        data class Running(val intent: String) : AgentState()
        data class Completed(val result: String) : AgentState()
        data class Error(val message: String) : AgentState()
    }
}
