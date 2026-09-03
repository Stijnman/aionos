package com.aionos.audit

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class AuditLogExporter(private val context: Context, private val auditLog: AuditLog) {
    
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
}
