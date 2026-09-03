package com.aionos.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted preferences storage using AndroidX Security.
 * All settings are encrypted at rest with AES-256-GCM.
 * 
 * For better testability, use the create() method or DI (AppContainer) instead of the singleton.
 */
class EncryptedPrefs private constructor(context: Context) {
    companion object {
        const val PREFS_FILE = "aionos_secure_prefs"
        const val KEY_AGENT_ENABLED = "agent_enabled"
        const val KEY_LLM_PROVIDER = "llm_provider"
        const val KEY_OLLAMA_HOST = "ollama_host"
        const val KEY_OLLAMA_MODEL = "ollama_model"
        const val KEY_CONFIRM_TIER_3 = "confirm_tier_3"
        const val KEY_AUDIT_RETENTION_DAYS = "audit_retention_days"
        const val KEY_FIRST_RUN = "first_run"
        const val KEY_VOICE_ENABLED = "voice_enabled"
        const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        const val KEY_LAST_CLEANUP = "last_cleanup_timestamp"

        @Volatile
        private var instance: EncryptedPrefs? = null

        /**
         * Get or create the singleton instance.
         * For better testability, consider using DI (AppContainer) instead.
         */
        fun getInstance(context: Context): EncryptedPrefs {
            return instance ?: synchronized(this) {
                instance ?: EncryptedPrefs(context.applicationContext).also { instance = it }
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
        fun create(context: Context): EncryptedPrefs {
            return EncryptedPrefs(context.applicationContext)
        }
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context, PREFS_FILE, masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var isAgentEnabled: Boolean
        get() = prefs.getBoolean(KEY_AGENT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AGENT_ENABLED, value).apply()

    var llmProvider: String
        get() = prefs.getString(KEY_LLM_PROVIDER, "ollama") ?: "ollama"
        set(value) = prefs.edit().putString(KEY_LLM_PROVIDER, value).apply()

    var ollamaHost: String
        get() = prefs.getString(KEY_OLLAMA_HOST, "http://192.168.1.1:11434") ?: "http://192.168.1.1:11434"
        set(value) = prefs.edit().putString(KEY_OLLAMA_HOST, value).apply()

    var ollamaModel: String
        get() = prefs.getString(KEY_OLLAMA_MODEL, "llama3.2") ?: "llama3.2"
        set(value) = prefs.edit().putString(KEY_OLLAMA_MODEL, value).apply()

    var confirmTier3: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_TIER_3, true)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_TIER_3, value).apply()

    var auditRetentionDays: Int
        get() = prefs.getInt(KEY_AUDIT_RETENTION_DAYS, 30)
        set(value) = prefs.edit().putInt(KEY_AUDIT_RETENTION_DAYS, value).apply()

    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN, value).apply()

    var isVoiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_VOICE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_VOICE_ENABLED, value).apply()

    var isOverlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    var lastCleanupTimestamp: Long
        get() = prefs.getLong(KEY_LAST_CLEANUP, 0)
        set(value) = prefs.edit().putLong(KEY_LAST_CLEANUP, value).apply()

    fun emergencyStop() {
        prefs.edit().apply {
            putBoolean(KEY_AGENT_ENABLED, false)
            putString(KEY_LLM_PROVIDER, "ollama")
            putBoolean(KEY_VOICE_ENABLED, false)
            putBoolean(KEY_OVERLAY_ENABLED, false)
            apply()
        }
    }
}
