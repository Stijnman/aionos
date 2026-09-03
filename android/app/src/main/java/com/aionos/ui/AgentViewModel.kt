package com.aionos.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aionos.action.AgentAction
import com.aionos.agent.AgentOrchestrator
import com.aionos.service.AgentAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AgentViewModel : ViewModel() {
    private var orchestrator: AgentOrchestrator? = null

    private val _agentState = MutableStateFlow<AgentState>(AgentState.Idle)
    val agentState: StateFlow<AgentState> = _agentState

    private val _transcript = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript

    private val _textInput = MutableStateFlow("")
    val textInput: StateFlow<String> = _textInput

    var showConfirmation by mutableStateOf(false)
        private set
    var pendingAction by mutableStateOf<AgentAction?>(null)
        private set

    fun bindService(service: AgentAccessibilityService) {
        if (orchestrator == null) {
            orchestrator = AgentOrchestrator(service.applicationContext, service).apply {
                initialize()
                service.confirmationCallback = { action ->
                    pendingAction = action
                    showConfirmation = true
                    waitForConfirmation()
                }
            }
            viewModelScope.launch {
                orchestrator?.state?.collect { state ->
                    _agentState.value = when (state) {
                        is AgentOrchestrator.AgentState.Idle -> AgentState.Idle
                        is AgentOrchestrator.AgentState.Listening -> AgentState.Listening
                        is AgentOrchestrator.AgentState.Running -> AgentState.Running(state.intent)
                        is AgentOrchestrator.AgentState.Completed -> AgentState.Completed(state.result)
                        is AgentOrchestrator.AgentState.Error -> AgentState.Error(state.message)
                    }
                }
            }
            viewModelScope.launch {
                orchestrator?.transcript?.collect { _transcript.value = it }
            }
        }
    }

    fun submitCommand(command: String) {
        _textInput.value = command
        orchestrator?.executeIntent(command)
    }

    fun startVoice() {
        orchestrator?.startVoiceCommand()
    }

    fun stopVoice() {
        orchestrator?.stopVoiceCommand()
    }

    fun confirmAction() {
        confirmationResult = true
        showConfirmation = false
    }

    fun denyAction() {
        confirmationResult = false
        showConfirmation = false
    }

    fun cancel() {
        orchestrator?.cancel()
    }

    fun emergencyStop() {
        orchestrator?.cancel()
        AgentAccessibilityService.instance?.emergencyStop()
    }

    override fun onCleared() {
        super.onCleared()
        orchestrator?.destroy()
        orchestrator = null
    }

    @Volatile
    private var confirmationResult: Boolean? = null

    private suspend fun waitForConfirmation(): Boolean {
        while (confirmationResult == null) {
            kotlinx.coroutines.delay(100)
        }
        val result = confirmationResult ?: false
        confirmationResult = null
        return result
    }

    sealed class AgentState {
        object Idle : AgentState()
        object Listening : AgentState()
        data class Running(val intent: String) : AgentState()
        data class Completed(val result: String) : AgentState()
        data class Error(val message: String) : AgentState()
    }
}
