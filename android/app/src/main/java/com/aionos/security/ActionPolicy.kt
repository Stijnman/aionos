package com.aionos.security

import com.aionos.action.AgentAction

object ActionPolicy {
    const val MAX_TEXT_LENGTH = 1000
    const val MAX_SCROLL_AMOUNT = 5000
    const val MAX_WAIT_MILLIS = 30_000L

    fun validate(action: AgentAction): List<String> = buildList {
        if (action.safetyTier == AgentAction.SafetyTier.TIER_4) {
            add("TIER_4 action is blocked")
        }
        when (action) {
            is AgentAction.Tap -> requireCoordinates(action.x, action.y)
            is AgentAction.LongPress -> requireCoordinates(action.x, action.y)
            is AgentAction.Swipe -> {
                requireCoordinates(action.startX, action.startY)
                requireCoordinates(action.endX, action.endY)
            }
            is AgentAction.Type -> if (action.text.length > MAX_TEXT_LENGTH) add("Text input exceeds $MAX_TEXT_LENGTH characters")
            is AgentAction.OpenApp -> if (!PACKAGE_NAME.matches(action.packageName)) add("Invalid Android package name")
            is AgentAction.Scroll -> if (action.amount !in 1..MAX_SCROLL_AMOUNT) add("Scroll amount is out of range")
            is AgentAction.Wait -> if (action.millis !in 0..MAX_WAIT_MILLIS) add("Wait duration is out of range")
            is AgentAction.PressKey, is AgentAction.ReadText, is AgentAction.Blocked -> Unit
        }
    }

    private fun MutableList<String>.requireCoordinates(x: Int, y: Int) {
        if (x !in 0..20_000 || y !in 0..20_000) add("Coordinates are outside the safe range")
    }

    private val PACKAGE_NAME = Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+")
}
