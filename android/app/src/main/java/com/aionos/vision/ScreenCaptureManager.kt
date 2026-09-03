package com.aionos.vision

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.coroutines.resume

/** Captures one in-memory frame only after the user grants MediaProjection consent. */
class ScreenCaptureManager(private val context: Context) {
    private val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null

    fun createConsentIntent(): Intent = projectionManager.createScreenCaptureIntent()

    fun setProjectionResult(resultCode: Int, data: Intent): Result<Unit> = runCatching {
        release()
        projection = projectionManager.getMediaProjection(resultCode, data)
            ?: error("MediaProjection permission was not granted")
    }

    suspend fun captureOnce(): Result<android.graphics.Bitmap> = withContext(Dispatchers.Default) {
        runCatching {
            val mediaProjection = projection ?: error("Screen capture permission is not active")
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val imageReader = ImageReader.newInstance(metrics.widthPixels, metrics.heightPixels, 0x1, 2)
            reader = imageReader
            val virtualDisplay = mediaProjection.createVirtualDisplay(
                "AionOS-Capture", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi,
                0, imageReader.surface, null, Handler(Looper.getMainLooper())
            )
            try {
                val image = awaitImage(imageReader)
                imageToBitmap(image, metrics.widthPixels, metrics.heightPixels)
            } finally {
                virtualDisplay.release()
                imageReader.close()
                reader = null
            }
        }
    }

    private suspend fun awaitImage(reader: ImageReader): Image = suspendCancellableCoroutine { continuation ->
        reader.setOnImageAvailableListener({ source ->
            source.acquireLatestImage()?.let { if (continuation.isActive) continuation.resume(it) else it.close() }
        }, Handler(Looper.getMainLooper()))
        continuation.invokeOnCancellation { reader.setOnImageAvailableListener(null, null) }
    }

    private fun imageToBitmap(image: Image, width: Int, height: Int): android.graphics.Bitmap {
        image.use {
            val plane = it.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * width
            val bitmap = android.graphics.Bitmap.createBitmap(
                width + rowPadding / pixelStride, height, android.graphics.Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            return android.graphics.Bitmap.createBitmap(bitmap, 0, 0, width, height).also { bitmap.recycle() }
        }
    }

    fun release() {
        reader?.close()
        reader = null
        projection?.stop()
        projection = null
    }
}
