package com.aionos.action

import android.graphics.Rect

/**
 * Sealed class representing all possible agent actions.
 * Every action carries metadata for safety classification and audit logging.
 */
sealed class AgentAction(
    open val safetyTier: SafetyTier,
    open val requiresConfirmation: Boolean = false
) {
    data class Tap(
        val x: Int,
        val y: Int,
        val nodeText: String? = null
    ) : AgentAction(SafetyTier.TIER_2, requiresConfirmation = false)

    data class LongPress(
        val x: Int,
        val y: Int,
        val nodeText: String? = null
    ) : AgentAction(SafetyTier.TIER_2)

    data class Type(
        val text: String,
        val isPasswordField: Boolean = false,
        val nodeText: String? = null
    ) : AgentAction(
        if (isPasswordField) SafetyTier.TIER_3 else SafetyTier.TIER_2,
        requiresConfirmation = isPasswordField
    )

    data class Scroll(
        val direction: Direction,
        val amount: Int = 500
    ) : AgentAction(SafetyTier.TIER_1)

    data class Swipe(
        val startX: Int,
        val startY: Int,
        val endX: Int,
        val endY: Int
    ) : AgentAction(SafetyTier.TIER_1)

    data class OpenApp(
        val packageName: String,
        val activityName: String? = null
    ) : AgentAction(SafetyTier.TIER_2)

    data class PressKey(
        val globalAction: GlobalAction
    ) : AgentAction(SafetyTier.TIER_1) {
        val keyCode: Int get() = globalAction.actionId
    }

    data class ReadText(
        val nodeBounds: Rect? = null
    ) : AgentAction(SafetyTier.TIER_1)

    data class Wait(
        val millis: Long = 1000
    ) : AgentAction(SafetyTier.TIER_1)

    data class Blocked(val reason: String = "blocked") : AgentAction(SafetyTier.TIER_4)

    enum class Direction { UP, DOWN, LEFT, RIGHT }

    enum class SafetyTier {
        TIER_1,  // Auto-execute: scroll, swipe, back, home, wait, read
        TIER_2,  // Log + execute: tap, type (non-password), open app
        TIER_3,  // Require confirmation: type in password fields, send, delete
        TIER_4   // Blocked: install APK, grant permissions, disable accessibility
    }

    enum class GlobalAction(val actionId: Int) {
        BACK(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK),
        HOME(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME),
        RECENTS(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS),
        NOTIFICATIONS(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS),
        QUICK_SETTINGS(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS),
        POWER_DIALOG(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_POWER_DIALOG),
        LOCK_SCREEN(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN),
        TAKE_SCREENSHOT(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
    }
}
