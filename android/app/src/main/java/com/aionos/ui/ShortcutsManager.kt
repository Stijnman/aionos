package com.aionos.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Manages app shortcuts for quick access to common actions.
 * Uses Android's ShortcutManager API (API 25+).
 */
@RequiresApi(Build.VERSION_CODES.N)
class ShortcutsManager(private val context: Context) {
    
    companion object {
        const val SHORTCUT_ID_VOICE = "voice_command"
        const val SHORTCUT_ID_TEXT = "text_command"
        const val SHORTCUT_ID_AUDIT = "view_audit"
        const val SHORTCUT_ID_SETTINGS = "settings"
    }
    
    private val shortcutManager: ShortcutManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.getSystemService(ShortcutManager::class.java)
        } else {
            null
        }
    }
    
    /**
     * Publishes dynamic shortcuts for the app.
     * These appear when long-pressing the app icon.
     */
    fun publishShortcuts() {
        if (shortcutManager == null) return
        
        val shortcuts = listOf(
            createVoiceShortcut(),
            createTextShortcut(),
            createAuditShortcut(),
            createSettingsShortcut()
        )
        
        shortcutManager?.dynamicShortcuts = shortcuts
    }
    
    /**
     * Updates a specific shortcut.
     */
    fun updateShortcut(shortcut: ShortcutInfo) {
        shortcutManager?.updateShortcuts(listOf(shortcut))
    }
    
    /**
     * Removes all dynamic shortcuts.
     */
    fun removeAllShortcuts() {
        shortcutManager?.removeAllDynamicShortcuts()
    }
    
    /**
     * Creates a shortcut for voice commands.
     */
    private fun createVoiceShortcut(): ShortcutInfo {
        return ShortcutInfo.Builder(context, SHORTCUT_ID_VOICE)
            .setShortLabel("Voice Command")
            .setLongLabel("Start voice command")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_btn_speak_now))
            .setIntent(
                Intent(context, com.aionos.MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_action", "voice")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            .setRank(0)
            .build()
    }
    
    /**
     * Creates a shortcut for text commands.
     */
    private fun createTextShortcut(): ShortcutInfo {
        return ShortcutInfo.Builder(context, SHORTCUT_ID_TEXT)
            .setShortLabel("Text Command")
            .setLongLabel("Enter text command")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_dialog_email))
            .setIntent(
                Intent(context, com.aionos.MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_action", "text")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            .setRank(1)
            .build()
    }
    
    /**
     * Creates a shortcut for viewing audit log.
     */
    private fun createAuditShortcut(): ShortcutInfo {
        return ShortcutInfo.Builder(context, SHORTCUT_ID_AUDIT)
            .setShortLabel("Audit Log")
            .setLongLabel("View action history")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_recent_history))
            .setIntent(
                Intent(context, com.aionos.MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_action", "audit")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            .setRank(2)
            .build()
    }
    
    /**
     * Creates a shortcut for settings.
     */
    private fun createSettingsShortcut(): ShortcutInfo {
        return ShortcutInfo.Builder(context, SHORTCUT_ID_SETTINGS)
            .setShortLabel("Settings")
            .setLongLabel("Open settings")
            .setIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_preferences))
            .setIntent(
                Intent(context, com.aionos.MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("shortcut_action", "settings")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            .setRank(3)
            .build()
    }
    
    /**
     * Reports a shortcut as used (for ranking).
     */
    fun reportShortcutUsed(shortcutId: String) {
        shortcutManager?.reportShortcutUsed(shortcutId)
    }
    
    /**
     * Checks if shortcuts are supported on this device.
     */
    fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && shortcutManager != null
    }
}
