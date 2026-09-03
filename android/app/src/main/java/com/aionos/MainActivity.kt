package com.aionos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aionos.audit.AuditLog
import com.aionos.audit.AuditLogExporter
import com.aionos.security.BiometricGate
import com.aionos.security.EncryptedPrefs
import com.aionos.service.AgentAccessibilityService
import com.aionos.service.OverlayBubbleService
import com.aionos.ui.AgentViewModel
import com.aionos.ui.ConfirmationDialog
import com.aionos.ui.EnhancedSettingsScreen
import com.aionos.ui.LanguageSettingsScreen
import com.aionos.ui.OnboardingDialog
import com.aionos.ui.PluginScreen
import com.aionos.ui.theme.AionosTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val viewModel: AgentViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AgentAccessibilityService.instance?.let { viewModel.bindService(it) }
        
        // Handle widget quick commands
        if (intent?.hasExtra("widget_command") == true) {
            val command = intent.getStringExtra("widget_command") ?: ""
            viewModel.submitCommand(command)
        }
        
        // Handle shortcut actions
        if (intent?.hasExtra("shortcut_action") == true) {
            val action = intent.getStringExtra("shortcut_action")
            when (action) {
                "voice" -> viewModel.startVoice()
                "text" -> { /* Show text input */ }
                "audit" -> { /* Navigate to audit */ }
                "settings" -> { /* Navigate to settings */ }
            }
        }
        
        setContent {
            AionosTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AionosApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AionosApp(viewModel: AgentViewModel) {
    val context = LocalContext.current
    val prefs = remember { EncryptedPrefs.getInstance(context) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showOnboarding by remember { mutableStateOf(prefs.isFirstRun) }
    val activity = context as? FragmentActivity
    val agentState by viewModel.agentState.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Enhanced tabs with new features
    val tabs = listOf(
        "Dashboard" to Icons.Default.Dashboard,
        "Audit Log" to Icons.Default.History,
        "Settings" to Icons.Default.Settings,
        "Plugins" to Icons.Default.Extension,
        "Languages" to Icons.Default.Language
    )

    if (showOnboarding) {
        OnboardingDialog {
            prefs.isFirstRun = false
            showOnboarding = false
        }
    }

    if (viewModel.showConfirmation && viewModel.pendingAction != null) {
        ConfirmationDialog(
            action = viewModel.pendingAction!!,
            onConfirm = { viewModel.confirmAction() },
            onDismiss = { viewModel.denyAction() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AionOS") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    val statusColor = when (agentState) {
                        is AgentViewModel.AgentState.Running -> MaterialTheme.colorScheme.primary
                        is AgentViewModel.AgentState.Listening -> MaterialTheme.colorScheme.tertiary
                        is AgentViewModel.AgentState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Box(modifier = Modifier.size(12.dp).padding(end = 16.dp), contentAlignment = Alignment.Center) {
                        Badge(containerColor = statusColor)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) },
                        selected = selectedTab == index,
                        onClick = {
                            if (index == 2 && activity != null) {
                                // Settings requires biometric authentication
                                val gate = BiometricGate(activity)
                                if (prefs.isBiometricLockEnabled && gate.canAuthenticate()) {
                                    gate.authenticate { unlocked ->
                                        if (unlocked) selectedTab = index
                                    }
                                } else {
                                    selectedTab = index
                                }
                            } else {
                                selectedTab = index
                            }
                        }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel, prefs, transcript)
                1 -> AuditLogScreen()
                2 -> EnhancedSettingsScreen(prefs) { selectedTab = 0 }
                3 -> PluginScreen()
                4 -> LanguageSettingsScreen { selectedTab = 0 }
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: AgentViewModel, prefs: EncryptedPrefs, transcript: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isAgentEnabled by remember { mutableStateOf(prefs.isAgentEnabled) }
    var isOverlayEnabled by remember { mutableStateOf(prefs.isOverlayEnabled) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var commandText by remember { mutableStateOf("") }
    val agentState by viewModel.agentState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isAgentEnabled) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isAgentEnabled) "Agent Active" else "Agent Paused",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = when (agentState) {
                            is AgentViewModel.AgentState.Running -> "Executing command..."
                            is AgentViewModel.AgentState.Listening -> "Listening..."
                            is AgentViewModel.AgentState.Error -> "Error occurred"
                            else -> if (isAgentEnabled) "Ready for commands" else "Tap to enable"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = isAgentEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            val enabledServices = Settings.Secure.getString(
                                context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                            ) ?: ""
                            if (!enabledServices.contains(context.packageName)) {
                                showAccessibilityDialog = true
                            } else {
                                prefs.isAgentEnabled = true
                                isAgentEnabled = true
                            }
                        } else {
                            prefs.isAgentEnabled = false
                            isAgentEnabled = false
                            viewModel.cancel()
                        }
                    }
                )
            }
        }

        OutlinedTextField(
            value = commandText,
            onValueChange = { commandText = it },
            label = { Text("What should I do?") },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row {
                    IconButton(onClick = {
                        if (agentState is AgentViewModel.AgentState.Listening) viewModel.stopVoice()
                        else viewModel.startVoice()
                    }) {
                        Icon(
                            if (agentState is AgentViewModel.AgentState.Listening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Voice"
                        )
                    }
                    IconButton(
                        onClick = {
                            if (commandText.isNotBlank()) {
                                viewModel.submitCommand(commandText)
                                commandText = ""
                            }
                        },
                        enabled = commandText.isNotBlank() && isAgentEnabled
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        )

        if (transcript.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val lines = transcript.trim().split("\n")
                    items(lines) { line ->
                        Text(text = line, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { 
                    prefs.emergencyStop()
                    isAgentEnabled = false
                    isOverlayEnabled = false
                    viewModel.emergencyStop()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Emergency Stop")
            }
            if (agentState is AgentViewModel.AgentState.Running) {
                Button(onClick = { viewModel.cancel() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Floating Bubble", style = MaterialTheme.typography.titleMedium)
                    Text("Quick access from any screen", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = isOverlayEnabled,
                    onCheckedChange = { enabled ->
                        isOverlayEnabled = enabled
                        prefs.isOverlayEnabled = enabled
                        if (enabled) OverlayBubbleService.start(context) else OverlayBubbleService.stop(context)
                    }
                )
            }
        }
    }

    if (showAccessibilityDialog) {
        AlertDialog(
            onDismissRequest = { showAccessibilityDialog = false },
            title = { Text("Enable Accessibility Service") },
            text = { Text("AionOS needs Accessibility permissions to control your device. Please enable it in system settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showAccessibilityDialog = false
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showAccessibilityDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun AuditLogScreen() {
    val context = LocalContext.current
    val auditLog = remember { AuditLog(context) }
    var entries by remember { mutableStateOf<List<AuditLog.AuditEntry>>(emptyList()) }
    var stats by remember { mutableStateOf<AuditLog.AuditStats?>(null) }

    LaunchedEffect(Unit) {
        entries = auditLog.getRecentActions(100)
        stats = auditLog.getStats()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        stats?.let { s ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCard("Total", s.totalActions.toString())
                StatCard("Success", s.successfulActions.toString())
                StatCard("Today", s.todayActions.toString())
            }
            Spacer(Modifier.height(16.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries) { entry -> AuditEntryCard(entry) }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun AuditEntryCard(entry: AuditLog.AuditEntry) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    entry.actionType,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (entry.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(entry.timestamp)),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            entry.actionDetail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            entry.error?.let {
                Text("Error: $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// Import CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
