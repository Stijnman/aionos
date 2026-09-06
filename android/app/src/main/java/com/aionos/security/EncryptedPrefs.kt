package com.aionos.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPrefs(context: Context) {
    companion object {
        const val PREFS_FILE = "aionos_secure_prefs"
        const val KEY_AGENT_ENABLED = "agent_enabled"
        const val KEY_LLM_PROVIDER = "llm_provider"
        const val KEY_OLLAMA_HOST = "ollama_host"
        const val KEY_OLLAMA_MODEL = "ollama_model"
        const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"
        const val KEY_OPENROUTER_MODEL = "openrouter_model"
        const val KEY_REMOTE_CONSENT = "remote_provider_consent"
        const val KEY_CONFIRM_TIER_3 = "confirm_tier_3"
        const val KEY_AUDIT_RETENTION_DAYS = "audit_retention_days"
        const val KEY_FIRST_RUN = "first_run"
        const val KEY_VOICE_ENABLED = "voice_enabled"
        const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        const val KEY_LAST_CLEANUP = "last_cleanup_timestamp"

        @Volatile private var instance: EncryptedPrefs? = null

        fun getInstance(context: Context): EncryptedPrefs = instance ?: synchronized(this) {
            instance ?: EncryptedPrefs(context.applicationContext).also { instance = it }
        }
    }

    private val masterKey = MasterKey.Builder(context)
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
        set(value) {
            if (value in setOf("ollama", "mediapipe", "openrouter")) {
                prefs.edit().putString(KEY_LLM_PROVIDER, value).apply()
            }
        }

    var ollamaHost: String
        get() = prefs.getString(KEY_OLLAMA_HOST, "http://192.168.1.1:11434") ?: "http://192.168.1.1:11434"
        set(value) {
            NetworkPolicy.validateOllamaHost(value).onSuccess { normalized ->
                prefs.edit().putString(KEY_OLLAMA_HOST, normalized).apply()
            }
        }

    var ollamaModel: String
        get() = prefs.getString(KEY_OLLAMA_MODEL, "llama3.2") ?: "llama3.2"
        set(value) = prefs.edit().putString(KEY_OLLAMA_MODEL, value.trim().take(100)).apply()

    var openRouterApiKey: String
        get() = prefs.getString(KEY_OPENROUTER_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_OPENROUTER_API_KEY, value.trim()).apply()

    var openRouterModel: String
        get() = prefs.getString(KEY_OPENROUTER_MODEL, "openai/gpt-4o-mini") ?: "openai/gpt-4o-mini"
        set(value) = prefs.edit().putString(KEY_OPENROUTER_MODEL, value.trim().take(120)).apply()

    var remoteProviderConsent: Boolean
        get() = prefs.getBoolean(KEY_REMOTE_CONSENT, false)
        set(value) = prefs.edit().putBoolean(KEY_REMOTE_CONSENT, value).apply()

    var confirmTier3: Boolean
        get() = prefs.getBoolean(KEY_CONFIRM_TIER_3, true)
        set(value) = prefs.edit().putBoolean(KEY_CONFIRM_TIER_3, value).apply()

    var auditRetentionDays: Int
        get() = prefs.getInt(KEY_AUDIT_RETENTION_DAYS, 30)
        set(value) = prefs.edit().putInt(KEY_AUDIT_RETENTION_DAYS, value.coerceIn(1, 3650)).apply()

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
            putBoolean(KEY_REMOTE_CONSENT, false)
            putString(KEY_OPENROUTER_API_KEY, "")
            putBoolean(KEY_VOICE_ENABLED, false)
            putBoolean(KEY_OVERLAY_ENABLED, false)
            apply()
        }
    }
}
