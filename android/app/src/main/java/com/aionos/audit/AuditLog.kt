package com.aionos.audit

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aionos.action.AgentAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * SQLite-based audit log for tracking all agent actions.
 * All data is stored locally on-device and never transmitted.
 * 
 * For better testability, use the constructor directly instead of relying on inheritance.
 */
class AuditLog private constructor(context: Context) : SQLiteOpenHelper(
    context, DATABASE_NAME, null, DATABASE_VERSION
) {
    companion object {
        const val DATABASE_NAME = "aionos_audit.db"
        const val DATABASE_VERSION = 2
        const val TABLE_ACTIONS = "actions"
        const val COL_ID = "id"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_ACTION_TYPE = "action_type"
        const val COL_ACTION_DETAIL = "action_detail"
        const val COL_TARGET_APP = "target_app"
        const val COL_SUCCESS = "success"
        const val COL_ERROR = "error"
        const val COL_DURATION_MS = "duration_ms"

        @Volatile
        private var instance: AuditLog? = null

        /**
         * Get or create the singleton instance.
         * For better testability, consider using DI (AppContainer) instead.
         */
        fun getInstance(context: Context): AuditLog {
            return instance ?: synchronized(this) {
                instance ?: AuditLog(context.applicationContext).also { instance = it }
            }
        }
        
        /**
         * Reset the singleton instance. Useful for testing.
         */
        fun resetInstance() {
            instance = null
        }
        
        /**
         * Create a new instance without using the singleton.
         * Recommended for testing and dependency injection.
         */
        fun create(context: Context): AuditLog {
            return AuditLog(context.applicationContext)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_ACTIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_ACTION_TYPE TEXT NOT NULL,
                $COL_ACTION_DETAIL TEXT,
                $COL_TARGET_APP TEXT,
                $COL_SUCCESS INTEGER NOT NULL,
                $COL_ERROR TEXT,
                $COL_DURATION_MS INTEGER
            )
        """.trimIndent())
        db.execSQL("CREATE INDEX idx_timestamp ON $TABLE_ACTIONS($COL_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try { db.execSQL("CREATE INDEX idx_timestamp ON $TABLE_ACTIONS($COL_TIMESTAMP)") } catch (_: Exception) {}
        }
    }

    suspend fun record(
        action: AgentAction,
        success: Boolean,
        targetApp: String? = null,
        error: String? = null,
        durationMs: Long? = null
    ) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put(COL_TIMESTAMP, System.currentTimeMillis())
            put(COL_ACTION_TYPE, action.javaClass.simpleName)
            put(COL_ACTION_DETAIL, action.toString().take(500))
            put(COL_TARGET_APP, targetApp)
            put(COL_SUCCESS, if (success) 1 else 0)
            put(COL_ERROR, error?.take(500))
            put(COL_DURATION_MS, durationMs)
        }
        writableDatabase.insert(TABLE_ACTIONS, null, values)
    }

    suspend fun getRecentActions(limit: Int = 50): List<AuditEntry> = withContext(Dispatchers.IO) {
        val entries = mutableListOf<AuditEntry>()
        val cursor = readableDatabase.query(
            TABLE_ACTIONS, null, null, null, null, null,
            "$COL_TIMESTAMP DESC", limit.toString()
        )
        cursor.use {
            while (it.moveToNext()) {
                entries.add(AuditEntry(
                    id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                    timestamp = it.getLong(it.getColumnIndexOrThrow(COL_TIMESTAMP)),
                    actionType = it.getString(it.getColumnIndexOrThrow(COL_ACTION_TYPE)),
                    actionDetail = it.getString(it.getColumnIndexOrThrow(COL_ACTION_DETAIL)),
                    targetApp = it.getString(it.getColumnIndexOrThrow(COL_TARGET_APP)),
                    success = it.getInt(it.getColumnIndexOrThrow(COL_SUCCESS)) == 1,
                    error = it.getString(it.getColumnIndexOrThrow(COL_ERROR)),
                    durationMs = it.getLong(it.getColumnIndexOrThrow(COL_DURATION_MS))
                ))
            }
        }
        entries
    }

    suspend fun getStats(): AuditStats = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val total = db.rawQuery("SELECT COUNT(*) FROM $TABLE_ACTIONS", null).use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }
        val successCount = db.rawQuery("SELECT COUNT(*) FROM $TABLE_ACTIONS WHERE $COL_SUCCESS = 1", null).use {
            if (it.moveToFirst()) it.getLong(0) else 0L
        }
        val todayStart = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val todayCount = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_ACTIONS WHERE $COL_TIMESTAMP > ?",
            arrayOf(todayStart.toString())
        ).use { if (it.moveToFirst()) it.getLong(0) else 0L }
        AuditStats(total, successCount, total - successCount, todayCount)
    }

    suspend fun exportAsCsv(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("id,timestamp,action_type,action_detail,target_app,success,error,duration_ms")
        val cursor = readableDatabase.query(TABLE_ACTIONS, null, null, null, null, null, "$COL_TIMESTAMP ASC")
        cursor.use {
            while (it.moveToNext()) {
                sb.appendLine(buildString {
                    append(it.getLong(0)); append(",")
                    append(formatTimestamp(it.getLong(1))); append(",")
                    append(escapeCsv(it.getString(2))); append(",")
                    append(escapeCsv(it.getString(3) ?: "")); append(",")
                    append(escapeCsv(it.getString(4) ?: "")); append(",")
                    append(if (it.getInt(5) == 1) "true" else "false"); append(",")
                    append(escapeCsv(it.getString(6) ?: "")); append(",")
                    append(it.getLong(7))
                })
            }
        }
        sb.toString()
    }

    suspend fun cleanupOldEntries(retentionDays: Int) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        writableDatabase.delete(TABLE_ACTIONS, "$COL_TIMESTAMP < ?", arrayOf(cutoff.toString()))
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        writableDatabase.delete(TABLE_ACTIONS, null, null)
    }

    private fun formatTimestamp(ts: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(ts))

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
        return if (cleaned.contains(",") || cleaned.contains("\"")) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else cleaned
    }

    data class AuditEntry(
        val id: Long, val timestamp: Long, val actionType: String,
        val actionDetail: String?, val targetApp: String?, val success: Boolean,
        val error: String?, val durationMs: Long
    )

    data class AuditStats(
        val totalActions: Long, val successfulActions: Long,
        val failedActions: Long, val todayActions: Long
    )
}
