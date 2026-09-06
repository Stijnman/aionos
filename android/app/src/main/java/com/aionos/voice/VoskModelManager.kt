package com.aionos.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.coroutines.coroutineContext

class VoskModelManager(private val context: Context) {
    val modelDirectory: File get() = File(context.filesDir, "vosk-model")
    val isInstalled: Boolean
        get() = File(modelDirectory, "am").exists() || File(modelDirectory, "conf").exists()

    suspend fun download(
        url: String,
        expectedSha256: String? = null,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        val staging = File(context.cacheDir, "vosk-model-${System.currentTimeMillis()}.zip")
        val extracted = File(context.cacheDir, "vosk-model-${System.currentTimeMillis()}.extracted")
        try {
            require(url.startsWith("https://")) { "Vosk model downloads must use HTTPS" }
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            try {
                connection.connect()
                if (connection.responseCode !in 200..299) error("Download failed: HTTP ${connection.responseCode}")
                val total = connection.contentLengthLong
                require(total <= MAX_DOWNLOAD_BYTES || total < 0) { "Model download exceeds the size limit" }
                val digest = MessageDigest.getInstance("SHA-256")
                var downloaded = 0L
                connection.inputStream.use { input -> staging.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        downloaded += count
                        require(downloaded <= MAX_DOWNLOAD_BYTES) { "Model download exceeds the size limit" }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                        onProgress(downloaded, total)
                    }
                } }
                expectedSha256?.let {
                    require(digest.digest().toHex().equals(it.trim(), ignoreCase = true)) { "Model checksum mismatch" }
                }
            } finally {
                connection.disconnect()
            }

            extracted.deleteRecursively()
            require(extracted.mkdirs()) { "Could not create extraction directory" }
            ZipInputStream(staging.inputStream()).use { zip ->
                while (true) {
                    coroutineContext.ensureActive()
                    val entry = zip.nextEntry ?: break
                    val target = File(extracted, entry.name).canonicalFile
                    require(target.path.startsWith(extracted.canonicalPath + File.separator)) { "Unsafe archive entry" }
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output -> zip.copyTo(output) }
                    }
                    zip.closeEntry()
                }
            }
            val root = extracted.listFiles()?.singleOrNull { it.isDirectory } ?: extracted
            val installed = File(context.filesDir, "vosk-model.new")
            installed.deleteRecursively()
            root.copyRecursively(installed, overwrite = true)
            require(File(installed, "am").exists() || File(installed, "conf").exists()) { "Archive is not a Vosk model" }
            modelDirectory.deleteRecursively()
            check(installed.renameTo(modelDirectory)) { "Could not install Vosk model atomically" }
            Result.success(modelDirectory)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            staging.delete()
            extracted.deleteRecursively()
            File(context.filesDir, "vosk-model.new").deleteRecursively()
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    companion object {
        private const val MAX_DOWNLOAD_BYTES = 200L * 1024L * 1024L
    }
}
