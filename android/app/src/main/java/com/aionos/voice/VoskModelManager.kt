package com.aionos.voice

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class VoskModelManager(private val context: Context) {
    val modelDirectory: File get() = File(context.filesDir, "vosk-model")
    val isInstalled: Boolean get() = File(modelDirectory, "am" ).exists() || File(modelDirectory, "conf").exists()

    suspend fun download(
        url: String,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(url.startsWith("https://")) { "Vosk model downloads must use HTTPS" }
            val staging = File(context.cacheDir, "vosk-model.download")
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 20_000
                readTimeout = 60_000
                instanceFollowRedirects = true
            }
            connection.connect()
            if (connection.responseCode !in 200..299) error("Download failed: HTTP ${connection.responseCode}")
            val total = connection.contentLengthLong
            var downloaded = 0L
            connection.inputStream.use { input -> staging.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    downloaded += count
                    onProgress(downloaded, total)
                }
            } }
            connection.disconnect()
            val extracted = File(context.cacheDir, "vosk-model.extracted").apply { deleteRecursively(); mkdirs() }
            ZipInputStream(staging.inputStream()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = File(extracted, entry.name).canonicalFile
                    require(target.path.startsWith(extracted.canonicalPath + File.separator)) { "Unsafe archive entry" }
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }
            val root = extracted.listFiles()?.singleOrNull { it.isDirectory } ?: extracted
            modelDirectory.deleteRecursively()
            root.copyRecursively(modelDirectory, overwrite = true)
            staging.delete(); extracted.deleteRecursively()
            modelDirectory
        }
    }
}
