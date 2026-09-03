package com.aionos.parser

import com.aionos.action.AgentAction
import com.aionos.action.SafeActionExecutor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionParserTest {
    private val parser = ActionParser()

    @Test fun parsesStrictActionArray() {
        val actions = parser.parse("[{\"action\":\"tap\",\"x\":540,\"y\":1200},{\"action\":\"scroll\",\"direction\":\"up\"}]")
        assertEquals(listOf(AgentAction.Tap(540, 1200), AgentAction.Scroll(AgentAction.Direction.UP)), actions)
    }

    @Test fun rejectsMalformedJsonAndUnknownActions() {
        assertTrue(parser.parse("not-json").isEmpty())
        assertTrue(parser.parse("[{\"action\":\"install_apk\"}]").isEmpty())
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
}
