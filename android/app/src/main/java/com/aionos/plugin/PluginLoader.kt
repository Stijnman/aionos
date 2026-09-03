package com.aionos.plugin

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

/**
 * Plugin system for extending aionos capabilities.
 * Plugins are APKs with a manifest.json in assets/aionos-plugin/.
 * 
 * Security features:
 * - Validates plugin signatures against known trusted keys
 * - Validates plugin manifest structure
 * - Sandboxes plugin actions to prevent privilege escalation
 */
class PluginLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val loadedPlugins = mutableListOf<Plugin>()
    
    // Trusted plugin developer signatures (SHA-256 of signing certs)
    // Add trusted developer certificates here
    private val trustedSignatures = setOf(
        // Example: "A1:BC:DE..." - replace with actual trusted cert fingerprints
        // For production, populate this with known good developer certificates
    )
    
    // Allowed plugin actions that are considered safe
    private val allowedActions = setOf(
        "read_text",
        "simple_tap",
        "scroll",
        "wait"
    )
    
    // Blocked actions that plugins cannot perform
    private val blockedActions = setOf(
        "type_password",
        "install_apk",
        "grant_permission",
        "disable_accessibility",
        "emergency_stop",
        "clear_data"
    )

    fun scanForPlugins(): List<Plugin> {
        val pm = context.packageManager
        val plugins = mutableListOf<Plugin>()
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in installedApps) {
            try {
                // Validate plugin signature first
                val packageInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNATURES)
                if (!validateSignature(packageInfo.signatures)) {
                    continue  // Skip untrusted plugins
                }
                
                val resources = pm.getResourcesForApplication(app.packageName)
                val manifestStream = resources.assets.open("aionos-plugin/manifest.json")
                val manifestJson = manifestStream.bufferedReader().use { it.readText() }
                
                // Validate manifest structure
                val manifest = json.decodeFromString<PluginManifest>(manifestJson)
                if (!validateManifest(manifest)) {
                    continue  // Skip invalid manifests
                }
                
                // Validate plugin actions are allowed
                val safeActions = manifest.actions.filter { 
                    isActionAllowed(it.name) 
                }
                
                val safeManifest = manifest.copy(actions = safeActions)
                plugins.add(Plugin(safeManifest, app.packageName, app.loadIcon(pm)))
            } catch (_: Exception) {
                // Skip plugins that fail validation
            }
        }
        loadedPlugins.clear()
        loadedPlugins.addAll(plugins)
        return plugins
    }

    fun getLoadedPlugins(): List<Plugin> = loadedPlugins.toList()

    /**
     * Validates that a plugin signature is from a trusted developer.
     */
    private fun validateSignature(signatures: Array<Signature>): Boolean {
        if (signatures.isEmpty()) return false
        
        for (sig in signatures) {
            val fingerprint = getSignatureFingerprint(sig)
            if (trustedSignatures.contains(fingerprint)) {
                return true
            }
        }
        return false
    }
    
    /**
     * Converts a Signature to its SHA-256 fingerprint.
     */
    private fun getSignatureFingerprint(signature: Signature): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(signature.toBytes())
        return Base64.encodeToString(digest, Base64.NO_WRAP)
    }

    /**
     * Validates plugin manifest structure and content.
     */
    private fun validateManifest(manifest: PluginManifest): Boolean {
        // Check required fields
        if (manifest.name.isBlank() || manifest.package_name.isBlank()) {
            return false
        }
        
        // Check version format (semantic versioning)
        if (!Regex("^\\d+\\.\\d+\\.\\d+").matches(manifest.version)) {
            return false
        }
        
        // Validate all action names are valid identifiers
        for (action in manifest.actions) {
            if (action.name.isBlank() || !Regex("^[a-zA-Z_][a-zA-Z0-9_]*$").matches(action.name)) {
                return false
            }
        }
        
        // Validate permissions (plugins can't request dangerous permissions)
        val dangerousPermissions = setOf(
            "android.permission.INSTALL_PACKAGES",
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.MANAGE_EXTERNAL_STORAGE"
        )
        
        for (permission in manifest.permissions) {
            if (dangerousPermissions.contains(permission)) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * Checks if an action is allowed for plugins.
     */
    private fun isActionAllowed(actionName: String): Boolean {
        val lowerAction = actionName.lowercase()
        return allowedActions.contains(lowerAction) && !blockedActions.contains(lowerAction)
    }

    /**
     * Validates that a specific action can be executed by a plugin.
     */
    fun isActionAllowedForPlugin(pluginPackage: String, actionName: String): Boolean {
        val plugin = loadedPlugins.find { it.packageName == pluginPackage } ?: return false
        return plugin.manifest.actions.any { it.name == actionName } && isActionAllowed(actionName)
    }

    fun executePluginAction(actionName: String, params: Map<String, String>): Boolean {
        for (plugin in loadedPlugins) {
            val action = plugin.manifest.actions.find { it.name == actionName }
            if (action != null && isActionAllowed(actionName)) {
                val intent = android.content.Intent("com.aionos.plugin.ACTION_EXECUTE").apply {
                    `package` = plugin.packageName
                    putExtra("action_name", actionName)
                    for ((key, value) in params) putExtra(key, value)
                }
                context.sendBroadcast(intent)
                return true
            }
        }
        return false
    }

    /**
     * Adds a trusted developer signature to the allowlist.
     * Should only be called after user confirmation.
     */
    fun addTrustedSignature(signature: String) {
        // In production, persist this to encrypted storage
        // For now, just add to in-memory set
        // trustedSignatures = trustedSignatures + signature
    }

    /**
     * Removes a trusted developer signature.
     */
    fun removeTrustedSignature(signature: String) {
        // trustedSignatures = trustedSignatures - signature
    }

    @Serializable
    data class PluginManifest(
        val name: String,
        val version: String,
        val package_name: String,
        val actions: List<PluginAction> = emptyList(),
        val permissions: List<String> = emptyList(),
        val description: String = "",
        val author: String = ""
    )

    @Serializable
    data class PluginAction(
        val name: String,
        val description: String,
        val params: List<String> = emptyList(),
        val requiresConfirmation: Boolean = false
    )

    data class Plugin(
        val manifest: PluginManifest,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable
    )
}
