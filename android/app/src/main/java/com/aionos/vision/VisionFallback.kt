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
 * Integrated with ScreenCaptureManager for automatic screenshot capture.
 */
class VisionFallback(private val context: Context) {

    private var objectDetector: ObjectDetector? = null
    private var isInitialized = false
    private val screenCaptureManager = ScreenCaptureManager(context)

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

    /**
     * Captures the current screen and detects elements using vision.
     * Requires MediaProjection permission to be granted.
     */
    suspend fun captureAndDetect(): Result<List<DetectedElement>> = withContext(Dispatchers.Default) {
        runCatching {
            val bitmap = screenCaptureManager.captureOnce().getOrThrow()
            val elements = detectElements(bitmap)
            bitmap.recycle()
            elements
        }
    }

    /**
     * Sets up MediaProjection from activity result and captures screen.
     */
    fun setupProjection(resultCode: Int, data: android.content.Intent): Result<Unit> {
        return screenCaptureManager.setProjectionResult(resultCode, data)
    }

    /**
     * Creates intent for MediaProjection consent.
     */
    fun createConsentIntent() = screenCaptureManager.createConsentIntent()

    fun shouldUseVisionFallback(tree: String): Boolean {
        return tree.trim() == "<tree/>" ||
               (tree.contains("android.webkit.WebView") && tree.lines().size < 5)
    }

    fun close() {
        objectDetector?.close()
        objectDetector = null
        isInitialized = false
        screenCaptureManager.release()
    }

    data class DetectedElement(val label: String, val confidence: Float, val bounds: Rect)
}
