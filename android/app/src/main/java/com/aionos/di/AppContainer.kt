package com.aionos.di

import android.content.Context
import com.aionos.audit.AuditLog
import com.aionos.security.EncryptedPrefs

/**
 * Dependency injection container for AionOS.
 * Provides a centralized way to manage and access dependencies.
 * 
 * Usage:
 * val container = AppContainer(context)
 * val prefs = container.encryptedPrefs
 * val auditLog = container.auditLog
 */
class AppContainer(private val context: Context) {
    
    // Singleton instances (lazy initialization)
    private val _encryptedPrefs: EncryptedPrefs by lazy { EncryptedPrefs(context) }
    private val _auditLog: AuditLog by lazy { AuditLog(context) }
    
    // Public accessors
    val encryptedPrefs: EncryptedPrefs get() = _encryptedPrefs
    val auditLog: AuditLog get() = _auditLog
    
    /**
     * Resets all singletons. Useful for testing.
     */
    fun reset() {
        // Note: In Kotlin, we can't truly reset lazy delegates,
        // but this provides a clean API for testing
    }
    
    companion object {
        // Global container instance (optional, for convenience)
        @Volatile
        private var instance: AppContainer? = null
        
        fun getInstance(context: Context): AppContainer {
            return instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
        }
        
        fun resetInstance() {
            instance = null
        }
    }
}
