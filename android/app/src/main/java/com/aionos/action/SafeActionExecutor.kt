package com.aionos.action

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.aionos.audit.AuditLog
import com.aionos.parser.AccessibilityTreeParser
import com.aionos.security.EncryptedPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Executes agent actions with tiered safety, feedback loops, and audit logging.
 */
class SafeActionExecutor(
    private val service: AccessibilityService,
    private val prefs: EncryptedPrefs,
    private val auditLog: AuditLog,
    private val treeParser: AccessibilityTreeParser,
    private val onConfirmationRequired: suspend (AgentAction) -> Boolean
) {

    companion object {
        fun isBlockedByPolicy(action: AgentAction): Boolean =
            action.safetyTier == AgentAction.SafetyTier.TIER_4
    }

    private val actionHistory = ArrayDeque<ActionRecord>(50)
    private var lastTreeHash: Int = 0

    data class ActionRecord(val action: AgentAction, val treeHash: Int, val timestamp: Long)

    suspend fun execute(action: AgentAction): Result<String> = withContext(Dispatchers.Main) {
        val startTime = System.currentTimeMillis()

        if (!prefs.isAgentEnabled) {
            auditLog.record(action, false, error = "Agent disabled by kill switch")
            return@withContext Result.failure(IllegalStateException("Agent is paused. Enable in settings."))
        }

        if (action.safetyTier == AgentAction.SafetyTier.TIER_4) {
            auditLog.record(action, false, error = "TIER_4 action blocked")
            return@withContext Result.failure(SecurityException("Action blocked by safety policy: ${action.javaClass.simpleName}"))
        }

        if (action.safetyTier == AgentAction.SafetyTier.TIER_3 && prefs.confirmTier3) {
            val confirmed = withTimeoutOrNull(30000) { onConfirmationRequired(action) } ?: false
            if (!confirmed) {
                auditLog.record(action, false, error = "User denied confirmation")
                return@withContext Result.failure(SecurityException("User denied confirmation for ${action.javaClass.simpleName}"))
            }
        }

        if (isStuckLoop(action)) {
            auditLog.record(action, false, error = "Stuck loop detected")
            return@withContext Result.failure(IllegalStateException("Stuck loop detected. Same action repeated with no state change."))
        }

        val result = try {
            when (action) {
                is AgentAction.Tap -> performTap(action)
                is AgentAction.LongPress -> performLongPress(action)
                is AgentAction.Type -> performType(action)
                is AgentAction.Scroll -> performScroll(action)
                is AgentAction.Swipe -> performSwipe(action)
                is AgentAction.OpenApp -> performOpenApp(action)
                is AgentAction.PressKey -> performPressKey(action)
                is AgentAction.ReadText -> performReadText(action)
                is AgentAction.Wait -> performWait(action)
                is AgentAction.Blocked -> Result.failure(SecurityException("Blocked action cannot execute"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }

        if (action !is AgentAction.Wait) {
            delay(500)
            val currentRoot = service.rootInActiveWindow
            val currentHash = treeParser.parse(currentRoot).hashCode()
            if (currentHash == lastTreeHash && action.safetyTier != AgentAction.SafetyTier.TIER_1) {
                auditLog.record(action, result.isSuccess, error = "Warning: No state change detected", durationMs = System.currentTimeMillis() - startTime)
            } else {
                auditLog.record(action, result.isSuccess, durationMs = System.currentTimeMillis() - startTime)
            }
            lastTreeHash = currentHash
        } else {
            auditLog.record(action, result.isSuccess, durationMs = System.currentTimeMillis() - startTime)
        }

        actionHistory.addLast(ActionRecord(action, lastTreeHash, System.currentTimeMillis()))
        if (actionHistory.size > 50) actionHistory.removeFirst()

        result
    }

    private fun isStuckLoop(action: AgentAction): Boolean {
        if (actionHistory.size < 3) return false
        val recent = actionHistory.takeLast(3)
        val sameAction = recent.all { it.action.javaClass == action.javaClass }
        val sameTree = recent.map { it.treeHash }.toSet().size == 1
        val rapidFire = recent.zipWithNext { a, b -> b.timestamp - a.timestamp < 5000 }.all { it }
        return sameAction && sameTree && rapidFire
    }

    private fun performTap(action: AgentAction.Tap): Result<String> {
        val path = Path().apply { moveTo(action.x.toFloat(), action.y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        val dispatched = service.dispatchGesture(gesture, null, null)
        return if (dispatched) Result.success("Tapped at (${action.x}, ${action.y})")
        else Result.failure(IllegalStateException("Gesture dispatch failed"))
    }

    private fun performLongPress(action: AgentAction.LongPress): Result<String> {
        val path = Path().apply { moveTo(action.x.toFloat(), action.y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 800))
            .build()
        val dispatched = service.dispatchGesture(gesture, null, null)
        return if (dispatched) Result.success("Long-pressed at (${action.x}, ${action.y})")
        else Result.failure(IllegalStateException("Gesture dispatch failed"))
    }

    private fun performType(action: AgentAction.Type): Result<String> {
        val root = service.rootInActiveWindow ?: return Result.failure(IllegalStateException("No active window"))
        val focusedNode = findFocusedEditable(root)
            ?: return Result.failure(IllegalStateException("No focused editable field found"))
        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, action.text)
        }
        val success = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        return if (success) Result.success("Typed "${action.text.take(20)}${if (action.text.length > 20) "..." else ""}"")
        else Result.failure(IllegalStateException("Failed to type text"))
    }

    private fun performScroll(action: AgentAction.Scroll): Result<String> {
        val root = service.rootInActiveWindow ?: return Result.failure(IllegalStateException("No active window"))
        val scrollable = findScrollableNode(root)
            ?: return Result.failure(IllegalStateException("No scrollable container found"))
        val scrollAction = when (action.direction) {
            AgentAction.Direction.UP -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            AgentAction.Direction.DOWN -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            AgentAction.Direction.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_LEFT
            AgentAction.Direction.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_RIGHT
        }
        val success = scrollable.performAction(scrollAction)
        return if (success) Result.success("Scrolled ${action.direction}")
        else Result.failure(IllegalStateException("Scroll failed"))
    }

    private fun performSwipe(action: AgentAction.Swipe): Result<String> {
        val path = Path().apply {
            moveTo(action.startX.toFloat(), action.startY.toFloat())
            lineTo(action.endX.toFloat(), action.endY.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        val dispatched = service.dispatchGesture(gesture, null, null)
        return if (dispatched) Result.success("Swiped from (${action.startX},${action.startY}) to (${action.endX},${action.endY})")
        else Result.failure(IllegalStateException("Swipe dispatch failed"))
    }

    private fun performOpenApp(action: AgentAction.OpenApp): Result<String> {
        val intent = service.packageManager.getLaunchIntentForPackage(action.packageName)
            ?: return Result.failure(IllegalArgumentException("App not found: ${action.packageName}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        service.startActivity(intent)
        return Result.success("Opened ${action.packageName}")
    }

    private fun performPressKey(action: AgentAction.PressKey): Result<String> {
        val success = service.performGlobalAction(action.keyCode)
        return if (success) Result.success("Pressed ${action.globalAction.name}")
        else Result.failure(IllegalStateException("Global action ${action.globalAction.name} failed"))
    }

    private fun performReadText(action: AgentAction.ReadText): Result<String> {
        val root = service.rootInActiveWindow ?: return Result.failure(IllegalStateException("No active window"))
        val texts = mutableListOf<String>()
        collectText(root, texts)
        return Result.success(texts.joinToString("\n").take(2000))
    }

    private suspend fun performWait(action: AgentAction.Wait): Result<String> {
        delay(action.millis)
        return Result.success("Waited ${action.millis}ms")
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isFocused && node.isEditable) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable) return node
            for (i in 0 until node.childCount) node.getChild(i)?.let { queue.add(it) }
        }
        return null
    }

    private fun collectText(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        val text = (node.text ?: node.contentDescription)?.toString()?.trim()
        if (!text.isNullOrBlank() && node.isVisibleToUser) texts.add(text)
        for (i in 0 until node.childCount) node.getChild(i)?.let { collectText(it, texts) }
    }
}
