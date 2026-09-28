package com.aionos.security

import com.aionos.action.AgentAction

/**
 * Password-field confirmation must be driven by [android.view.accessibility.AccessibilityNodeInfo],
 * not by the LLM JSON flag alone.
 *
 * The model `isPasswordField` value may only increase caution. When the focused editable node
 * reports [android.view.accessibility.AccessibilityNodeInfo.isPassword], confirmation is required
 * even if the model omitted or denied the flag.
 */
object PasswordFieldGuard {

    /**
     * Effective password classification: node signal is authoritative for elevation.
     * LLM flag alone can still request confirmation, but can never clear a password node.
     */
    fun effectiveIsPasswordField(llmIsPasswordField: Boolean, nodeIsPassword: Boolean): Boolean =
        nodeIsPassword || llmIsPasswordField

    /**
     * Returns [action] unchanged, or a copy elevated to password/TIER_3 when the node is a password field.
     */
    fun resolveTypeAction(action: AgentAction.Type, nodeIsPassword: Boolean): AgentAction.Type {
        val effective = effectiveIsPasswordField(action.isPasswordField, nodeIsPassword)
        return if (effective == action.isPasswordField) action
        else action.copy(isPasswordField = true)
    }

    fun needsConfirmation(action: AgentAction): Boolean =
        action.safetyTier == AgentAction.SafetyTier.TIER_3 || action.requiresConfirmation

    fun killSwitchBlocksExecution(agentEnabled: Boolean): Boolean = !agentEnabled
}
