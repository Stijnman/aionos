package com.aionos.audit

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuditLogExporter(private val context: Context, private val auditLog: AuditLog) {
    suspend fun exportCsv(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val csv = auditLog.exportAsCsv()
            context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
                output.writer(Charsets.UTF_8).use { writer -> writer.write(csv) }
            } ?: error("Unable to open selected destination")
            csv.toByteArray(Charsets.UTF_8).size.toLong()
        }
    }
}
