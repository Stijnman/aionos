package com.aionos.plugin

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Plugin system for extending aionos capabilities.
 * Plugins are APKs with a manifest.json in assets/aionos-plugin/.
 */
class PluginLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val loadedPlugins = mutableListOf<Plugin>()

    fun scanForPlugins(): List<Plugin> {
        val pm = context.packageManager
        val plugins = mutableListOf<Plugin>()
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (app in installedApps) {
            try {
                val resources = pm.getResourcesForApplication(app.packageName)
                val manifestStream = resources.assets.open("aionos-plugin/manifest.json")
                val manifestJson = manifestStream.bufferedReader().use { it.readText() }
                val manifest = json.decodeFromString<PluginManifest>(manifestJson)
                plugins.add(Plugin(manifest, app.packageName, app.loadIcon(pm)))
            } catch (_: Exception) {}
        }
        loadedPlugins.clear()
        loadedPlugins.addAll(plugins)
        return plugins
    }

    fun getLoadedPlugins(): List<Plugin> = loadedPlugins.toList()

    fun executePluginAction(actionName: String, params: Map<String, String>): Boolean {
        for (plugin in loadedPlugins) {
            val action = plugin.manifest.actions.find { it.name == actionName }
            if (action != null) {
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

    @Serializable
    data class PluginManifest(
        val name: String,
        val version: String,
        val package_name: String,
        val actions: List<PluginAction> = emptyList(),
        val permissions: List<String> = emptyList()
    )

    @Serializable
    data class PluginAction(val name: String, val description: String, val params: List<String> = emptyList())

    data class Plugin(
        val manifest: PluginManifest,
        val packageName: String,
        val icon: android.graphics.drawable.Drawable
    )
}
