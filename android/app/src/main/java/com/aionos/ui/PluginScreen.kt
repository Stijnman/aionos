package com.aionos.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aionos.plugin.PluginLoader

/**
 * Plugin management screen for discovering and executing plugins.
 */
@Composable
fun PluginScreen() {
    val context = LocalContext.current
    val loader = remember { PluginLoader(context) }
    var plugins by remember { mutableStateOf<List<PluginLoader.Plugin>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var lastScanTime by remember { mutableStateOf<java.util.Date?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        scanPlugins(loader, context) { resultPlugins, scanDate ->
            plugins = resultPlugins
            lastScanTime = scanDate
            isLoading = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Plugins", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(
                onClick = {
                    isLoading = true
                    scanPlugins(loader, context) { resultPlugins, scanDate ->
                        plugins = resultPlugins
                        lastScanTime = scanDate
                        isLoading = false
                    }
                }
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan")
                    Spacer(Modifier.width(4.dp))
                    Text("Rescan")
                }
            }
        }
        
        // Last scan info
        lastScanTime?.let { date ->
            Text(
                "Last scanned: ${java.text.SimpleDateFormat.getDateTimeInstance().format(date)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        if (isLoading && plugins.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(1f))
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Scanning for plugins...")
                Spacer(Modifier.weight(1f))
            }
        } else if (plugins.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(16.dp))
                Text("No compatible AionOS plugins found.")
                Text("Install plugin APKs with aionos-plugin/manifest.json in assets/", 
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
            }
        } else {
            Text("${plugins.size} plugin(s) found", style = MaterialTheme.typography.bodyMedium)
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(plugins) { plugin ->
                    PluginCard(
                        plugin = plugin,
                        onActionExecute = { actionName, params ->
                            val success = loader.executePluginAction(actionName, params)
                            message = if (success) {
                                "Executed ${actionName}"
                            } else {
                                "Failed to execute ${actionName}"
                            }
                        }
                    )
                }
            }
        }
        
        // Status message
        message?.let { msg ->
            Card(
                colors = MaterialTheme.colorScheme.primaryContainer.copy()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

/**
 * Scans for plugins and updates state.
 */
private suspend fun scanPlugins(
    loader: PluginLoader,
    context: Context,
    onComplete: (List<PluginLoader.Plugin>, java.util.Date) -> Unit
) {
    val result = loader.scanForPlugins()
    onComplete(result, java.util.Date())
}

/**
 * Card displaying a single plugin with its actions.
 */
@Composable
private fun PluginCard(
    plugin: PluginLoader.Plugin,
    onActionExecute: (String, Map<String, String>) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Plugin header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(plugin.manifest.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "v${plugin.manifest.version} • ${plugin.manifest.author.takeIf { it.isNotBlank() } ?: "Unknown"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    painter = androidx.compose.ui.graphics.asImageBitmap(plugin.icon.toBitmap()),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            // Plugin description
            if (plugin.manifest.description.isNotBlank()) {
                Text(
                    plugin.manifest.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            // Plugin actions
            if (plugin.manifest.actions.isNotEmpty()) {
                Text(
                    "Actions:",
                    style = MaterialTheme.typography.labelMedium
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    plugin.manifest.actions.forEach { action ->
                        Button(
                            onClick = { onActionExecute(action.name, emptyMap()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(action.name)
                        }
                        if (action.requiresConfirmation) {
                            Text(
                                "Requires confirmation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            } else {
                Text(
                    "No actions available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Plugin package info
            Text(
                plugin.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// Extension to convert Drawable to Bitmap
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

fun Drawable.toComposeBitmap(): androidx.compose.ui.graphics.ImageBitmap {
    return toBitmap().asImageBitmap()
}
