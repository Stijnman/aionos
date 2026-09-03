package com.aionos.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Handles intent-based actions for launching apps and performing system operations.
 * Provides a safe way to launch apps by package name or intent URI.
 */
class IntentActionHandler(private val context: Context) {
    
    private val packageManager: PackageManager = context.packageManager
    
    /**
     * Launches an app by its package name.
     * 
     * @param packageName The package name of the app to launch
     * @param activityName Optional activity name to launch
     * @return Result indicating success or failure
     */
    fun launchApp(packageName: String, activityName: String? = null): Result<Unit> {
        return runCatching {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                ?: throw PackageManager.NameNotFoundException("Package $packageName not found")
            
            activityName?.let { name ->
                launchIntent.setClassName(packageName, name)
            }
            
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(launchIntent)
        }
    }
    
    /**
     * Launches an app by intent URI.
     * Supports http, https, tel, sms, mailto, and custom schemes.
     * 
     * @param uriString The URI to open
     * @return Result indicating success or failure
     */
    fun launchUri(uriString: String): Result<Unit> {
        return runCatching {
            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Opens a specific system settings screen.
     * 
     * @param settingsAction The settings action to open
     * @return Result indicating success or failure
     */
    fun openSettings(settingsAction: String): Result<Unit> {
        return runCatching {
            val intent = Intent(settingsAction)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
    
    /**
     * Launches the app picker for a specific action.
     * 
     * @param action The intent action (e.g., Intent.ACTION_SEND)
     * @param type Optional MIME type
     * @param extras Optional intent extras
     * @return Result indicating success or failure
     */
    fun launchPicker(action: String, type: String? = null, extras: Intent.() -> Unit = {}): Result<Unit> {
        return runCatching {
            val intent = Intent(action)
            type?.let { intent.type = it }
            intent.extras?.apply(extras)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(Intent.createChooser(intent, null))
        }
    }
    
    /**
     * Checks if an app with the given package name is installed.
     * 
     * @param packageName The package name to check
     * @return true if the app is installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Gets the launch intent for an app by package name.
     * 
     * @param packageName The package name
     * @return The launch intent or null if not found
     */
    fun getLaunchIntent(packageName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }
    
    /**
     * Gets a list of all installed apps that can handle a specific intent.
     * 
     * @param intent The intent to check
     * @return List of package names that can handle the intent
     */
    fun getAppsForIntent(intent: Intent): List<String> {
        val resolveInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfoList.map { it.activityInfo.packageName }.distinct()
    }
    
    /**
     * Common settings actions for easy access.
     */
    object SettingsActions {
        const val ACCESSIBILITY = android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS
        const val WIFI = android.provider.Settings.ACTION_WIFI_SETTINGS
        const val BLUETOOTH = android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
        const val LOCATION = android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
        const val BATTERY = android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
        const val STORAGE = android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS
        const val APPS = android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS
        const val SECURITY = android.provider.Settings.ACTION_SECURITY_SETTINGS
        const val DISPLAY = android.provider.Settings.ACTION_DISPLAY_SETTINGS
        const val SOUND = android.provider.Settings.ACTION_SOUND_SETTINGS
    }
    
    /**
     * Common URI schemes for easy launching.
     */
    object UriSchemes {
        const val HTTP = "http://"
        const val HTTPS = "https://"
        const val TEL = "tel:"
        const val SMS = "sms:"
        const val MAILTO = "mailto:"
        const val GEO = "geo:"
        const val MARKET = "market://"
    }
}
