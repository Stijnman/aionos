package com.aionos.voice

import android.content.Context
import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Manages multiple Vosk language models for multi-language voice input.
 * Supports downloading, caching, and switching between different language models.
 */
class MultiLanguageVoskManager(private val context: Context) {
    
    companion object {
        // Supported language models and their download URLs
        val SUPPORTED_MODELS = mapOf(
            "en-us" to "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip",
            "en-in" to "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.42.zip",
            "fr" to "https://alphacephei.com/vosk/models/vosk-model-small-fr-0.42.zip",
            "de" to "https://alphacephei.com/vosk/models/vosk-model-small-de-0.15.zip",
            "es" to "https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip",
            "it" to "https://alphacephei.com/vosk/models/vosk-model-small-it-0.42.zip",
            "pt" to "https://alphacephei.com/vosk/models/vosk-model-small-pt-0.42.zip",
            "ru" to "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.42.zip",
            "zh" to "https://alphacephei.com/vosk/models/vosk-model-small-cn-0.42.zip",
            "ja" to "https://alphacephei.com/vosk/models/vosk-model-small-ja-0.42.zip"
        )
        
        // Language display names
        val LANGUAGE_NAMES = mapOf(
            "en-us" to "English (US)",
            "en-in" to "English (India)",
            "fr" to "French",
            "de" to "German",
            "es" to "Spanish",
            "it" to "Italian",
            "pt" to "Portuguese",
            "ru" to "Russian",
            "zh" to "Chinese",
            "ja" to "Japanese"
        )
    }
    
    private val modelCache = mutableMapOf<String, Model>()
    private val modelDirectories = mutableMapOf<String, File>()
    
    private val baseCacheDir: File by lazy {
        File(context.cacheDir, "vosk-models").apply { mkdirs() }
    }
    
    /**
     * Gets the model directory for a specific language.
     */
    private fun getModelDirectory(languageCode: String): File {
        return modelDirectories.getOrPut(languageCode) {
            File(baseCacheDir, "vosk-model-$languageCode").apply { mkdirs() }
        }
    }
    
    /**
     * Checks if a language model is installed.
     */
    fun isModelInstalled(languageCode: String): Boolean {
        val modelDir = getModelDirectory(languageCode)
        return File(modelDir, "am").exists() || File(modelDir, "conf").exists()
    }
    
    /**
     * Gets the list of installed language models.
     */
    fun getInstalledLanguages(): List<String> {
        return SUPPORTED_MODELS.keys.filter { isModelInstalled(it) }
    }
    
    /**
     * Gets the list of available (downloadable) language models.
     */
    fun getAvailableLanguages(): List<String> {
        return SUPPORTED_MODELS.keys.toList()
    }
    
    /**
     * Gets the display name for a language code.
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return LANGUAGE_NAMES[languageCode] ?: languageCode
    }
    
    /**
     * Loads a model for the specified language.
     * Returns the model or throws if not installed.
     */
    fun loadModel(languageCode: String): Model {
        return modelCache.getOrPut(languageCode) {
            val modelDir = getModelDirectory(languageCode)
            if (!isModelInstalled(languageCode)) {
                throw IllegalStateException("Model for language $languageCode is not installed")
            }
            Model(modelDir.path)
        }
    }
    
    /**
     * Unloads a model to free memory.
     */
    fun unloadModel(languageCode: String) {
        modelCache[languageCode]?.close()
        modelCache.remove(languageCode)
    }
    
    /**
     * Unloads all models.
     */
    fun unloadAllModels() {
        modelCache.values.forEach { it.close() }
        modelCache.clear()
    }
    
    /**
     * Downloads a language model.
     * 
     * @param languageCode The language code to download
     * @param onProgress Progress callback (downloaded bytes, total bytes)
     * @return Result with the model directory or error
     */
    suspend fun downloadModel(
        languageCode: String,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val url = SUPPORTED_MODELS[languageCode]
                ?: throw IllegalArgumentException("Unsupported language code: $languageCode")
            
            require(url.startsWith("https://")) { "Vosk model downloads must use HTTPS" }
            
            val staging = File(context.cacheDir, "vosk-model-$languageCode.download")
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            
            connection.connect()
            if (connection.responseCode !in 200..299) {
                error("Download failed: HTTP ${connection.responseCode}")
            }
            
            val total = connection.contentLengthLong
            var downloaded = 0L
            
            connection.inputStream.use { input ->
                staging.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloaded += count
                        onProgress(downloaded, total)
                    }
                }
            }
            
            connection.disconnect()
            
            // Extract the zip file
            val extracted = File(context.cacheDir, "vosk-model-$languageCode.extracted").apply {
                deleteRecursively()
                mkdirs()
            }
            
            ZipInputStream(staging.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = File(extracted, entry.name).canonicalFile
                    
                    // Security check: prevent path traversal
                    require(target.path.startsWith(extracted.canonicalPath + File.separator)) {
                        "Unsafe archive entry"
                    }
                    
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }
            
            // Move to final location
            val root = extracted.listFiles()?.singleOrNull { it.isDirectory } ?: extracted
            val modelDir = getModelDirectory(languageCode)
            modelDir.deleteRecursively()
            root.copyRecursively(modelDir, overwrite = true)
            
            // Cleanup
            staging.delete()
            extracted.deleteRecursively()
            
            // Clear cached model to force reload
            modelCache.remove(languageCode)
            
            modelDir
        }
    }
    
    /**
     * Deletes a downloaded language model.
     */
    fun deleteModel(languageCode: String): Boolean {
        unloadModel(languageCode)
        val modelDir = getModelDirectory(languageCode)
        return modelDir.deleteRecursively()
    }
    
    /**
     * Gets the current language model in use.
     */
    fun getCurrentLanguage(): String? {
        return modelCache.keys.firstOrNull()
    }
    
    /**
     * Cleans up all cached models and directories.
     */
    fun cleanupAll() {
        unloadAllModels()
        baseCacheDir.deleteRecursively()
        baseCacheDir.mkdirs()
        modelDirectories.clear()
    }
    
    /**
     * Gets the total disk space used by all models.
     */
    fun getTotalModelSize(): Long {
        return baseCacheDir.walk()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
    
    /**
     * Gets information about a specific model.
     */
    data class ModelInfo(
        val languageCode: String,
        val displayName: String,
        val isInstalled: Boolean,
        val size: Long? = null
    )
    
    /**
     * Gets information about all supported models.
     */
    fun getAllModelInfo(): List<ModelInfo> {
        return SUPPORTED_MODELS.keys.map { code ->
            val modelDir = getModelDirectory(code)
            val size = if (modelDir.exists()) {
                modelDir.walk().filter { it.isFile }.sumOf { it.length() }
            } else {
                null
            }
            ModelInfo(
                languageCode = code,
                displayName = getLanguageDisplayName(code),
                isInstalled = isModelInstalled(code),
                size = size
            )
        }
    }
}
