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
                require(manifest.package_name == app.packageName) { "Plugin manifest package mismatch" }
                require(manifest.name.length in 1..120 && manifest.version.length in 1..60) { "Invalid plugin identity" }
                require(manifest.actions.size <= 50) { "Too many plugin actions" }
                require(manifest.actions.all { it.name.matches(ACTION_NAME) && it.params.size <= 50 }) {
                    "Invalid plugin action declaration"
                }
                plugins.add(Plugin(manifest, app.packageName, app.loadIcon(pm)))
            } catch (_: Exception) {
                // Invalid or incompatible plugins are ignored and never made executable.
            }
        }
        loadedPlugins.clear()
        loadedPlugins.addAll(plugins)
        return plugins
    }

    fun getLoadedPlugins(): List<Plugin> = loadedPlugins.toList()

    fun executePluginAction(plugin: Plugin, actionName: String, params: Map<String, String>): Result<Unit> = runCatching {
        require(loadedPlugins.any { it.packageName == plugin.packageName }) { "Plugin is not loaded" }
        val action = plugin.manifest.actions.firstOrNull { it.name == actionName }
            ?: error("Action is not declared by this plugin")
        require(actionName.matches(ACTION_NAME)) { "Invalid plugin action name" }
        require(params.keys.all { it in action.params }) { "Undeclared plugin parameter" }
        require(params.size <= action.params.size) { "Too many plugin parameters" }
        require(params.values.all { it.length <= 1000 }) { "Plugin parameter is too large" }
        val intent = android.content.Intent("com.aionos.plugin.ACTION_EXECUTE").apply {
            `package` = plugin.packageName
            putExtra("action_name", actionName)
            params.forEach { (key, value) -> putExtra(key, value.take(1000)) }
        }
        context.sendBroadcast(intent)
    }

    @Deprecated("Use the explicitly targeted overload")
    fun executePluginAction(actionName: String, params: Map<String, String>): Boolean =
        loadedPlugins.firstNotNullOfOrNull { plugin ->
            executePluginAction(plugin, actionName, params).getOrNull()?.let { true }
        } ?: false

    companion object {
        private val ACTION_NAME = Regex("[A-Za-z][A-Za-z0-9_.-]{0,63}")
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
