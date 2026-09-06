package com.aionos.vision

import android.graphics.Rect
import org.junit.Assert.assertTrue
import org.junit.Test

class VisionActionMapperTest {
    @Test fun rejectsLowConfidenceDetections() {
        val element = VisionFallback.DetectedElement("button", 0.79f, Rect(0, 0, 100, 100))
        assertTrue(VisionActionMapper.proposeTap(element, 1080, 1920).isFailure)
    }

    @Test fun acceptsHighConfidenceInBoundsDetection() {
        val element = VisionFallback.DetectedElement("button", 0.95f, Rect(0, 0, 100, 100))
        val action = VisionActionMapper.proposeTap(element, 1080, 1920).getOrThrow()
        assertTrue(action.x in 0 until 1080 && action.y in 0 until 1920)
    }

    @Test fun rejectsHighConfidenceNonInteractiveObject() {
        val element = VisionFallback.DetectedElement("dog", 0.99f, Rect(0, 0, 100, 100))
        assertTrue(VisionActionMapper.proposeTap(element, 1080, 1920).isFailure)
    }
}
