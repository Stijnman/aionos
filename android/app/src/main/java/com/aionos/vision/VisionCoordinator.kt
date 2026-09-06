package com.aionos.vision

import android.graphics.Bitmap
import com.aionos.action.AgentAction

/** Runs one-shot local vision analysis and returns proposals for the normal policy pipeline. */
class VisionCoordinator(private val fallback: VisionFallback) {
    suspend fun proposeTaps(bitmap: Bitmap): Result<List<AgentAction.Tap>> = runCatching {
        try {
            val elements = fallback.detectElements(bitmap)
            elements.mapNotNull { element ->
                VisionActionMapper.proposeTap(element, bitmap.width, bitmap.height).getOrNull()
            }.distinctBy { "${it.x}:${it.y}" }
        } finally {
            bitmap.recycle()
        }
    }

    fun close() = fallback.close()
}
