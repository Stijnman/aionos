package com.aionos.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Vision fallback for when AccessibilityNodeInfo tree is empty or unhelpful.
 * Uses MediaPipe Object Detection to identify interactive elements from screenshots.
 */
class VisionFallback(private val context: Context) {

    private var objectDetector: ObjectDetector? = null
    private var isInitialized = false

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("efficientdet-lite0.tflite")
                .build()
            val options = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(0.5f)
                .setMaxResults(10)
                .build()
            objectDetector = ObjectDetector.createFromOptions(context, options)
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun detectElements(bitmap: Bitmap): List<DetectedElement> = withContext(Dispatchers.Default) {
        if (!isInitialized) initialize().getOrThrow()
        val detector = objectDetector ?: return@withContext emptyList()
        val results = detector.detect(bitmap)
        results.detections().map { detection ->
            val bbox = detection.boundingBox()
            DetectedElement(
                label = detection.categories().firstOrNull()?.categoryName() ?: "unknown",
                confidence = detection.categories().firstOrNull()?.score() ?: 0f,
                bounds = Rect(bbox.left.toInt(), bbox.top.toInt(), bbox.right.toInt(), bbox.bottom.toInt())
            )
        }
    }

    fun shouldUseVisionFallback(tree: String): Boolean {
        return tree.trim() == "<tree/>" ||
               (tree.contains("android.webkit.WebView") && tree.lines().size < 5)
    }

    fun close() {
        objectDetector?.close()
        objectDetector = null
        isInitialized = false
    }

    data class DetectedElement(val label: String, val confidence: Float, val bounds: Rect)
}
