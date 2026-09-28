package com.aionos.security

import com.aionos.action.AgentAction
import com.aionos.parser.ActionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordFieldGuardTest {

    @Test
    fun omittingLlmPasswordFlagCannotBypassWhenNodeIsPassword() {
        // Attack surface: model omits isPasswordField / sets false
        assertFalse(
            PasswordFieldGuard.effectiveIsPasswordField(
                llmIsPasswordField = false,
                nodeIsPassword = false
            )
        )
        // Node reports password → must elevate regardless of LLM flag
        assertTrue(
            PasswordFieldGuard.effectiveIsPasswordField(
                llmIsPasswordField = false,
                nodeIsPassword = true
            )
        )
    }

    @Test
    fun resolveTypeActionElevatesOmittedFlagToTier3() {
        val parsed = ActionParser().parse(
            "[{\"action\":\"type\",\"text\":\"secret\"}]"
        ).single() as AgentAction.Type

        assertFalse(parsed.isPasswordField)
        assertFalse(parsed.requiresConfirmation)
        assertEquals(AgentAction.SafetyTier.TIER_2, parsed.safetyTier)

        val elevated = PasswordFieldGuard.resolveTypeAction(parsed, nodeIsPassword = true)
        assertTrue(elevated.isPasswordField)
        assertTrue(elevated.requiresConfirmation)
        assertEquals(AgentAction.SafetyTier.TIER_3, elevated.safetyTier)
        assertTrue(PasswordFieldGuard.needsConfirmation(elevated))
    }

    @Test
    fun llmFlagAloneStillRequestsConfirmationButNodeFalseDoesNotElevateFurther() {
        val typed = AgentAction.Type("x", isPasswordField = true)
        assertTrue(PasswordFieldGuard.needsConfirmation(typed))
        val resolved = PasswordFieldGuard.resolveTypeAction(typed, nodeIsPassword = false)
        assertTrue(resolved.isPasswordField)
        assertTrue(PasswordFieldGuard.needsConfirmation(resolved))
    }

    @Test
    fun killSwitchBlocksExecutionWhenAgentDisabled() {
        assertTrue(PasswordFieldGuard.killSwitchBlocksExecution(agentEnabled = false))
        assertFalse(PasswordFieldGuard.killSwitchBlocksExecution(agentEnabled = true))
    }

    @Test
    fun nonPasswordTypeDoesNotRequireConfirmation() {
        val action = AgentAction.Type("hello", isPasswordField = false)
        val resolved = PasswordFieldGuard.resolveTypeAction(action, nodeIsPassword = false)
        assertFalse(PasswordFieldGuard.needsConfirmation(resolved))
        assertEquals(AgentAction.SafetyTier.TIER_2, resolved.safetyTier)
    }
}
