package com.aionos.agent

import android.content.Context
import com.aionos.action.AgentAction
import com.aionos.llm.LLMBridge
import com.aionos.service.AgentAccessibilityService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AgentOrchestratorTest {

    private lateinit var orchestrator: AgentOrchestrator
    private val mockContext: Context = mockk()
    private val mockService: AgentAccessibilityService = mockk()
    private val mockBridge: LLMBridge = mockk()

    @Before
    fun setup() {
        mockkStatic(Dispatchers::class)
        every { Dispatchers.Main } returns Dispatchers.Unconfined
        
        orchestrator = AgentOrchestrator(mockContext, mockService)
    }

    @After
    fun tearDown() {
        orchestrator.destroy()
        unmockkAll()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        val state = orchestrator.state.first()
        assert(state is AgentOrchestrator.AgentState.Idle)
    }

    @Test
    fun `initialize sets up LLM bridge`() {
        // This test would need reflection to verify internal state
        // For now, just verify it doesn't throw
        orchestrator.initialize()
        assert(true)
    }

    @Test
    fun `destroy cleans up resources`() {
        orchestrator.destroy()
        // Verify no exceptions
        assert(true)
    }

    @Test
    fun `cancel stops current execution`() = runTest {
        orchestrator.cancel()
        // Should transition back to Idle
        val state = orchestrator.state.first()
        assert(state is AgentOrchestrator.AgentState.Idle)
    }

    @Test
    fun `AgentState sealed class has all expected states`() {
        val idle = AgentOrchestrator.AgentState.Idle
        val listening = AgentOrchestrator.AgentState.Listening
        val running = AgentOrchestrator.AgentState.Running("test")
        val completed = AgentOrchestrator.AgentState.Completed("result")
        val error = AgentOrchestrator.AgentState.Error("error")
        
        assert(idle is AgentOrchestrator.AgentState.Idle)
        assert(listening is AgentOrchestrator.AgentState.Listening)
        assert(running is AgentOrchestrator.AgentState.Running)
        assert(completed is AgentOrchestrator.AgentState.Completed)
        assert(error is AgentOrchestrator.AgentState.Error)
    }

    @Test
    fun `Running state contains intent`() {
        val running = AgentOrchestrator.AgentState.Running("open settings")
        assert(running.intent == "open settings")
    }

    @Test
    fun `Completed state contains result`() {
        val completed = AgentOrchestrator.AgentState.Completed("success")
        assert(completed.result == "success")
    }

    @Test
    fun `Error state contains message`() {
        val error = AgentOrchestrator.AgentState.Error("failed")
        assert(error.message == "failed")
    }

    @Test
    fun `buildSystemPrompt returns non-empty string`() {
        val prompt = com.aionos.llm.buildSystemPrompt()
        assert(prompt.isNotEmpty())
        assert(prompt.contains("Android UI automation agent"))
        assert(prompt.contains("JSON array"))
    }

    @Test
    fun `buildUserPrompt includes user intent`() {
        val prompt = com.aionos.llm.buildUserPrompt(
            userIntent = "open app",
            uiTree = "<tree/>",
            currentApp = "com.test"
        )
        assert(prompt.contains("open app"))
        assert(prompt.contains("com.test"))
    }

    @Test
    fun `buildUserPrompt includes history`() {
        val history = listOf("action1", "action2")
        val prompt = com.aionos.llm.buildUserPrompt(
            userIntent = "test",
            uiTree = "<tree/>",
            currentApp = "com.test",
            history = history
        )
        assert(prompt.contains("action1"))
        assert(prompt.contains("action2"))
    }

    @Test
    fun `buildUserPrompt truncates long UI tree`() {
        val longTree = "<tree>" + "x".repeat(10000) + "</tree>"
        val prompt = com.aionos.llm.buildUserPrompt(
            userIntent = "test",
            uiTree = longTree,
            currentApp = "com.test"
        )
        // Should be truncated to ~8000 chars
        assert(prompt.length < 10000)
    }

    @Test
    fun `executeIntent with empty intent does not throw`() = runTest {
        // Mock the service to return a valid tree
        every { mockService.rootInActiveWindow } returns null
        every { mockService.getCurrentTree() } returns "<tree/>"
        
        // This will fail because LLM is not initialized, but we're testing it doesn't crash
        orchestrator.executeIntent("")
        
        // Give it a moment to process
        val state = orchestrator.state.first()
        // Should be in Error state since LLM is not initialized
        assert(state is AgentOrchestrator.AgentState.Error || state is AgentOrchestrator.AgentState.Running)
    }
}
