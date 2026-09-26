package com.aionos.parser

import com.aionos.action.AgentAction
import com.aionos.action.SafeActionExecutor
import com.aionos.security.PasswordFieldGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionParserTest {
    private val parser = ActionParser()

    @Test fun parsesStrictActionArray() {
        val actions = parser.parse("[{\"action\":\"tap\",\"x\":540,\"y\":1200},{\"action\":\"scroll\",\"direction\":\"up\"}]")
        assertEquals(listOf(AgentAction.Tap(540, 1200), AgentAction.Scroll(AgentAction.Direction.UP)), actions)
    }

    @Test fun rejectsMalformedJsonUnknownAndIncompleteActions() {
        assertTrue(parser.parse("not-json").isEmpty())
        assertTrue(parser.parse("[{\"action\":\"install_apk\"}]").isEmpty())
        assertTrue(parser.parse("[{\"action\":\"tap\",\"x\":10}]").isEmpty())
    }

    @Test fun validatesBlockedAndUnsafeInputs() {
        val blocked = AgentAction.Type("secret", isPasswordField = true)
        val invalid = AgentAction.Tap(-1, 20)
        assertTrue(parser.validate(listOf(blocked, invalid)) is ActionParser.ValidationResult.Invalid)
    }

    @Test fun executorPolicyBlocksTierFour() {
        val allowed = AgentAction.Scroll(AgentAction.Direction.DOWN)
        val blocked = AgentAction.Blocked()
        assertTrue(!SafeActionExecutor.isBlockedByPolicy(allowed))
        assertTrue(SafeActionExecutor.isBlockedByPolicy(blocked))
    }

    @Test fun typeWithoutPasswordFlagDefaultsToNonConfirmingUntilNodeGuard() {
        val actions = parser.parse("[{\"action\":\"type\",\"text\":\"hunter2\"}]")
        val type = actions.single() as AgentAction.Type
        assertFalse(type.isPasswordField)
        assertFalse(type.requiresConfirmation)
        // Guard must still elevate when AccessibilityNodeInfo says password
        val elevated = PasswordFieldGuard.resolveTypeAction(type, nodeIsPassword = true)
        assertTrue(elevated.requiresConfirmation)
        assertEquals(AgentAction.SafetyTier.TIER_3, elevated.safetyTier)
    }

    @Test fun explicitFalsePasswordFlagStillElevatesFromNode() {
        val actions = parser.parse(
            "[{\"action\":\"type\",\"text\":\"secret\",\"isPasswordField\":false}]"
        )
        val type = actions.single() as AgentAction.Type
        assertFalse(type.isPasswordField)
        assertTrue(
            PasswordFieldGuard.needsConfirmation(
                PasswordFieldGuard.resolveTypeAction(type, nodeIsPassword = true)
            )
        )
    }
}
