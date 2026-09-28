package com.aionos.vision

import com.aionos.action.AgentAction

object VisionActionMapper {
    fun proposeTap(
        element: VisionFallback.DetectedElement,
        screenWidth: Int,
        screenHeight: Int,
        minimumConfidence: Float = 0.80f
    ): Result<AgentAction.Tap> = runCatching {
        require(element.confidence >= minimumConfidence) { "Vision confidence is below the safe threshold" }
        require(element.label.lowercase() in INTERACTIVE_LABELS) { "Detection is not a recognized interactive control" }
        require(screenWidth > 0 && screenHeight > 0) { "Invalid screen dimensions" }
        // Use Rect's public fields rather than Android framework helper methods so this
        // safety mapper remains deterministic in local JVM unit tests as well as on-device.
        val centerX = element.bounds.left + (element.bounds.right - element.bounds.left) / 2
        val centerY = element.bounds.top + (element.bounds.bottom - element.bounds.top) / 2
        require(centerX in 0 until screenWidth && centerY in 0 until screenHeight) {
            "Detected element is outside the screen bounds"
        }
        AgentAction.Tap(centerX, centerY, nodeText = element.label.take(120))
    }

    private val INTERACTIVE_LABELS = setOf(
        "button", "link", "text", "input", "checkbox", "radio", "switch", "menu", "icon"
    )
}
