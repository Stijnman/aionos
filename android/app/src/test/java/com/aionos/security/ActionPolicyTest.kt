package com.aionos.security

import com.aionos.action.AgentAction
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionPolicyTest {
    @Test fun rejectsInvalidCoordinatesAndPackages() {
        assertTrue(ActionPolicy.validate(AgentAction.Tap(-1, 40)).isNotEmpty())
        assertTrue(ActionPolicy.validate(AgentAction.OpenApp("not a package")).isNotEmpty())
    }

    @Test fun rejectsUnboundedWaitAndScroll() {
        assertTrue(ActionPolicy.validate(AgentAction.Wait(ActionPolicy.MAX_WAIT_MILLIS + 1)).isNotEmpty())
        assertTrue(ActionPolicy.validate(AgentAction.Scroll(AgentAction.Direction.DOWN, ActionPolicy.MAX_SCROLL_AMOUNT + 1)).isNotEmpty())
    }

    @Test fun acceptsSafeValues() {
        assertTrue(ActionPolicy.validate(AgentAction.Tap(100, 200)).isEmpty())
        assertTrue(ActionPolicy.validate(AgentAction.OpenApp("com.example.app")).isEmpty())
    }
}
