package com.aionos.audit

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * Exports audit log data using Storage Access Framework (SAF).
 * SAF provides a system picker UI that lets users choose where to save files
 * without requiring any storage permissions.
 */
class AuditLogExporter(private val context: Context, private val auditLog: AuditLog) {
    
    /**
     * Creates an intent for the user to select where to export the CSV file.
     * Uses Storage Access Framework (SAF) which doesn't require any permissions.
     * 
     * @param suggestedFilename The suggested filename (without .csv extension)
     * @return Intent to launch with ActivityResultContracts
     */
    fun createExportIntent(suggestedFilename: String = "aionos-audit-export"): Intent {
        return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "$suggestedFilename.csv")
            addCategory(Intent.CATEGORY_OPENABLE)
        }
    }
    
    /**
     * ActivityResultContract for export operations.
     * Use with rememberLauncherForActivityResult in Compose.
     */
    val exportDocumentContract = ActivityResultContracts.CreateDocument("text/csv")
    
    /**
     * Exports audit log as CSV to the specified URI.
     * Returns a Flow that emits progress updates and the final result.
     */
    fun exportCsv(uri: Uri): Flow<ExportProgress> = flow {
        emit(ExportProgress.Started)
        
        try {
            val csv = withContext(Dispatchers.IO) {
                auditLog.exportAsCsv()
            }
            
            emit(ExportProgress.GeneratingData(csv.length))
            
            val byteArray = csv.toByteArray(Charsets.UTF_8)
            emit(ExportProgress.WritingFile(byteArray.size.toLong()))
            
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    output.writer(Charsets.UTF_8).use { writer -> 
                        writer.write(csv)
                    }
                } ?: error("Unable to open selected destination")
            }
            
            emit(ExportProgress.Completed(byteArray.size.toLong()))
        } catch (e: Exception) {
            emit(ExportProgress.Failed(e.message ?: "Unknown error"))
        }
    }
    
    /**
     * Exports audit log to a SAF URI with progress tracking.
     * This is the recommended method for new code.
     */
    fun exportToUri(uri: Uri): Flow<ExportProgress> = exportCsv(uri)
    
    /**
     * Simple suspend function for backward compatibility.
     * For new code, use the Flow-based exportCsv() for progress tracking.
     */
    @Deprecated("Use exportCsv() Flow version for progress tracking")
    suspend fun exportCsvBlocking(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val csv = auditLog.exportAsCsv()
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.writer(Charsets.UTF_8).use { writer -> writer.write(csv) }
            } ?: error("Unable to open selected destination")
            csv.toByteArray(Charsets.UTF_8).size.toLong()
        }
    }
    
    sealed class ExportProgress {
        object Started : ExportProgress()
        data class GeneratingData(val estimatedSize: Int) : ExportProgress()
        data class WritingFile(val bytesWritten: Long) : ExportProgress()
        data class Completed(val totalBytes: Long) : ExportProgress()
        data class Failed(val error: String) : ExportProgress()
    }
    
    /**
     * Companion object for static utility methods.
     */
    companion object {
        const val MIME_TYPE_CSV = "text/csv"
        const val DEFAULT_FILENAME = "aionos-audit-export"
    }
}
