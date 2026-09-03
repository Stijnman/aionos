package com.aionos.ui

import android.content.Context
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aionos.voice.MultiLanguageVoskManager
import kotlinx.coroutines.launch

/**
 * Language settings screen for managing Vosk language models.
 */
@Composable
fun LanguageSettingsScreen(onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { MultiLanguageVoskManager(context) }
    
    var modelInfos by remember { mutableStateOf(manager.getAllModelInfo()) }
    var downloadingLanguage by remember { mutableStateOf<String?>(null) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var message by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        modelInfos = manager.getAllModelInfo()
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Language Models", style = MaterialTheme.typography.headlineSmall)
            }
            Button(onClick = onNavigateUp) {
                Text("Back")
            }
        }
        
        // Current language
        val currentLang = manager.getCurrentLanguage()
        currentLang?.let { lang ->
            Card(
                colors = MaterialTheme.colorScheme.primaryContainer.copy()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Current: ${manager.getLanguageDisplayName(lang)}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        
        // Total size
        val totalSize = manager.getTotalModelSize()
        if (totalSize > 0) {
            Text(
                "Total models size: ${formatBytes(totalSize)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        // Downloading indicator
        downloadingLanguage?.let { lang ->
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Downloading ${manager.getLanguageDisplayName(lang)}...")
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { downloadProgress })
                    Text("${(downloadProgress * 100).toInt()}%")
                }
            }
        }
        
        // Language list
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(modelInfos) { modelInfo ->
                LanguageModelCard(
                    modelInfo = modelInfo,
                    isDownloading = downloadingLanguage == modelInfo.languageCode,
                    onDownload = { languageCode ->
                        scope.launch {
                            downloadingLanguage = languageCode
                            downloadProgress = 0f
                            
                            manager.downloadModel(languageCode) { downloaded, total ->
                                downloadProgress = if (total > 0) downloaded.toFloat() / total else 0f
                            }.onSuccess {
                                message = "Downloaded ${modelInfo.displayName}"
                                downloadingLanguage = null
                                modelInfos = manager.getAllModelInfo()
                            }.onFailure { e ->
                                message = "Failed to download ${modelInfo.displayName}: ${e.message}"
                                downloadingLanguage = null
                            }
                        }
                    },
                    onDelete = { languageCode ->
                        if (manager.deleteModel(languageCode)) {
                            message = "Deleted ${modelInfo.displayName}"
                            modelInfos = manager.getAllModelInfo()
                        } else {
                            message = "Failed to delete ${modelInfo.displayName}"
                        }
                    }
                )
            }
        }
        
        // Status message
        message?.let { msg ->
            Card {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(msg)
                }
            }
        }
    }
}

/**
 * Card for a single language model.
 */
@Composable
private fun LanguageModelCard(
    modelInfo: MultiLanguageVoskManager.ModelInfo,
    isDownloading: Boolean,
    onDownload: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(modelInfo.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        modelInfo.languageCode,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                if (modelInfo.isInstalled) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Installed",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // Size info
            modelInfo.size?.let { size ->
                Text(
                    "Size: ${formatBytes(size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (modelInfo.isInstalled) {
                    IconButton(
                        onClick = { onDelete(modelInfo.languageCode) },
                        enabled = !isDownloading
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Button(
                        onClick = { onDownload(modelInfo.languageCode) },
                        enabled = !isDownloading
                    ) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats bytes to human-readable string.
 */
private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> "%.2f GB".format(bytes.toDouble() / (1024 * 1024 * 1024))
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes.toDouble() / (1024 * 1024))
        bytes >= 1024 -> "%.2f KB".format(bytes.toDouble() / 1024)
        else -> "$bytes B"
    }
}
