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
        require(screenWidth > 0 && screenHeight > 0) { "Invalid screen dimensions" }
        val centerX = element.bounds.centerX()
        val centerY = element.bounds.centerY()
        require(centerX in 0 until screenWidth && centerY in 0 until screenHeight) {
            "Detected element is outside the screen bounds"
        }
        AgentAction.Tap(centerX, centerY, nodeText = element.label.take(120))
    }
}
