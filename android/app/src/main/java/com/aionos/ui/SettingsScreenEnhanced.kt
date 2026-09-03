package com.aionos.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aionos.audit.AuditLog
import com.aionos.audit.AuditLogExporter
import com.aionos.security.BiometricGate
import com.aionos.security.EncryptedPrefs
import com.aionos.voice.VoskModelManager
import kotlinx.coroutines.launch

/**
 * Enhanced settings screen with biometric lock and improved organization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSettingsScreen(prefs: EncryptedPrefs, onNavigateUp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = remember { AuditLogExporter(context, AuditLog(context)) }
    val modelManager = remember { VoskModelManager(context) }
    
    var modelStatus by remember { mutableStateOf(if (modelManager.isInstalled) "Vosk model installed" else "Vosk model not installed") }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var showBiometricLock by remember { mutableStateOf(false) }
    var requiresBiometric by remember { mutableStateOf(false) }
    var showAccessibilityReminder by remember { mutableStateOf(false) }
    
    val activity = context as? FragmentActivity
    
    // Check if accessibility is enabled
    val isAccessibilityEnabled = remember {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        enabledServices.contains(context.packageName)
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
            OutlinedButton(onClick = onNavigateUp) {
                Text("Back")
            }
        }
        
        // Biometric Lock Section
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Biometric Lock", style = MaterialTheme.typography.titleMedium)
                        Text("Require authentication to access settings", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = requiresBiometric,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                activity?.let { act ->
                                    val gate = BiometricGate(act)
                                    if (gate.canAuthenticate()) {
                                        gate.authenticate { success ->
                                            if (success) {
                                                requiresBiometric = true
                                                prefs.isBiometricLockEnabled = true
                                            }
                                        }
                                    }
                                }
                            } else {
                                requiresBiometric = false
                                prefs.isBiometricLockEnabled = false
                            }
                        }
                    )
                }
                
                if (requiresBiometric) {
                    Icon(
                        Icons.Default.Fingerprint,
                        contentDescription = "Biometric protected",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Divider()
        
        // Agent Configuration Section
        Text("Agent Configuration", style = MaterialTheme.typography.titleMedium)
        
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
                    Text("Agent Enabled")
                    Switch(
                        checked = prefs.isAgentEnabled,
                        onCheckedChange = { prefs.isAgentEnabled = it }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Overlay Bubble")
                    Switch(
                        checked = prefs.isOverlayEnabled,
                        onCheckedChange = { prefs.isOverlayEnabled = it }
                    )
                }
                
                if (!isAccessibilityEnabled) {
                    Card(
                        colors = MaterialTheme.colorScheme.errorContainer.copy()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Accessibility Service Not Enabled",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            TextButton(onClick = { showAccessibilityReminder = true }) {
                                Text("Enable", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }
        }
        
        Divider()
        
        // LLM Configuration Section
        Text("LLM Configuration", style = MaterialTheme.typography.titleMedium)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var llmProvider by remember { mutableStateOf(prefs.llmProvider) }
                var ollamaHost by remember { mutableStateOf(prefs.ollamaHost) }
                var ollamaModel by remember { mutableStateOf(prefs.ollamaModel) }
                
                OutlinedTextField(
                    value = llmProvider,
                    onValueChange = { 
                        llmProvider = it
                        prefs.llmProvider = it
                    },
                    label = { Text("LLM Provider") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (llmProvider == "ollama") {
                    OutlinedTextField(
                        value = ollamaHost,
                        onValueChange = { 
                            ollamaHost = it
                            prefs.ollamaHost = it
                        },
                        label = { Text("Ollama Host") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = ollamaModel,
                        onValueChange = { 
                            ollamaModel = it
                            prefs.ollamaModel = it
                        },
                        label = { Text("Model Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Divider()
        
        // Voice Input Section
        Text("Voice Input", style = MaterialTheme.typography.titleMedium)
        
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
                    Text("Voice Enabled")
                    Switch(
                        checked = prefs.isVoiceEnabled,
                        onCheckedChange = { prefs.isVoiceEnabled = it }
                    )
                }
                
                Text(modelStatus, style = MaterialTheme.typography.bodySmall)
                
                Button(
                    onClick = {
                        scope.launch {
                            modelStatus = "Downloading Vosk model..."
                            modelStatus = modelManager.download("https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip")
                                .fold({ "Vosk model installed" }, { "Model download failed: ${it.message}" })
                        }
                    },
                    enabled = !modelManager.isInstalled
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Download English Voice Model")
                }
            }
        }
        
        Divider()
        
        // Audit Log Section
        Text("Audit Log", style = MaterialTheme.typography.titleMedium)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var retentionDays by remember { mutableStateOf(prefs.auditRetentionDays.toString()) }
                
                OutlinedTextField(
                    value = retentionDays,
                    onValueChange = {
                        retentionDays = it
                        it.toIntOrNull()?.let { days -> prefs.auditRetentionDays = days }
                    },
                    label = { Text("Retention (days)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Button(
                    onClick = {
                        val intent = exporter.createExportIntent()
                        activity?.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export Audit Log (SAF)")
                }
                
                exportStatus?.let { 
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Divider()
        
        // Appearance Section
        Text("Appearance", style = MaterialTheme.typography.titleMedium)
        
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var useDarkMode by remember { mutableStateOf(false) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Dark Mode")
                    }
                    Switch(
                        checked = useDarkMode,
                        onCheckedChange = { useDarkMode = it }
                    )
                }
            }
        }
        
        Divider()
        
        // Security Section
        Text("Security", style = MaterialTheme.typography.titleMedium)
        
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm Destructive Actions")
                    }
                    Switch(
                        checked = prefs.confirmTier3,
                        onCheckedChange = { prefs.confirmTier3 = it }
                    )
                }
                
                Button(
                    onClick = {
                        prefs.emergencyStop()
                    },
                    colors = MaterialTheme.colorScheme.errorContainer.copy()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("Emergency Stop", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
    
    // Accessibility reminder dialog
    if (showAccessibilityReminder) {
        AlertDialog(
            onDismissRequest = { showAccessibilityReminder = false },
            title = { Text("Enable Accessibility Service") },
            text = { Text("AionOS needs Accessibility permissions to control your device. Please enable it in system settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showAccessibilityReminder = false
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityReminder = false }) { Text("Cancel") }
            }
        )
    }
}

// Extension property for biometric lock preference
var EncryptedPrefs.isBiometricLockEnabled: Boolean
    get() = getBoolean("biometric_lock_enabled", false)
    set(value) = edit().putBoolean("biometric_lock_enabled", value).apply()
