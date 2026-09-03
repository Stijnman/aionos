package com.aionos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aionos.audit.AuditLog
import com.aionos.audit.AuditLogExporter
import com.aionos.security.BiometricGate
import com.aionos.security.EncryptedPrefs
import com.aionos.plugin.PluginLoader
import com.aionos.vision.ScreenCaptureManager
import com.aionos.voice.VoskModelManager
import com.aionos.service.AgentAccessibilityService
import com.aionos.service.OverlayBubbleService
import com.aionos.ui.AgentViewModel
import com.aionos.ui.ConfirmationDialog
import com.aionos.ui.OnboardingDialog
import com.aionos.ui.theme.AionosTheme

class MainActivity : FragmentActivity() {
    private val viewModel: AgentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AgentAccessibilityService.instance?.let { viewModel.bindService(it) }
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
    val tabs = listOf("Dashboard", "Audit Log", "Settings", "Plugins")

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
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        icon = {
                            when (index) {
                                0 -> Icon(Icons.Default.Dashboard, contentDescription = null)
                                1 -> Icon(Icons.Default.History, contentDescription = null)
                                2 -> Icon(Icons.Default.Settings, contentDescription = null)
                                else -> Icon(Icons.Default.Extension, contentDescription = null)
                            }
                        },
                        label = { Text(title) },
                        selected = selectedTab == index,
                        onClick = {
                            if (index == 2 && activity != null) {
                                BiometricGate(activity).authenticate { unlocked -> if (unlocked) selectedTab = index }
                            } else selectedTab = index
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel, prefs, transcript)
                1 -> AuditLogScreen()
                2 -> SettingsScreen(prefs)
                3 -> PluginScreen()
            }
        }
    }
}

@Composable
fun DashboardScreen(viewModel: AgentViewModel, prefs: EncryptedPrefs, transcript: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureManager = remember { ScreenCaptureManager(context) }
    var captureStatus by remember { mutableStateOf<String?>(null) }
    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            captureManager.setProjectionResult(result.resultCode, result.data!!).onSuccess {
                scope.launch { captureStatus = captureManager.captureOnce().fold({ "Screenshot captured in memory (${it.width}×${it.height})" }, { "Capture failed: ${it.message}" }) }
            }.onFailure { captureStatus = "Capture permission failed: ${it.message}" }
        } else captureStatus = "Screenshot permission was cancelled"
    }
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

        captureStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { captureLauncher.launch(captureManager.createConsentIntent()) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Capture")
            }
            Button(
                onClick = {
                    prefs.emergencyStop()
                    isAgentEnabled = false
                    isOverlayEnabled = false
                    viewModel.emergencyStop()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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

@Composable
fun SettingsScreen(prefs: EncryptedPrefs) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exporter = remember { AuditLogExporter(context, AuditLog(context)) }
    val modelManager = remember { VoskModelManager(context) }
    var modelStatus by remember { mutableStateOf(if (modelManager.isInstalled) "Vosk model installed" else "Vosk model not installed") }
    var exportStatus by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch {
            exportStatus = exporter.exportCsv(uri).fold({ "Exported $it bytes" }, { "Export failed: ${it.message}" })
        }
    }
    var llmProvider by remember { mutableStateOf(prefs.llmProvider) }
    var ollamaHost by remember { mutableStateOf(prefs.ollamaHost) }
    var ollamaModel by remember { mutableStateOf(prefs.ollamaModel) }
    var confirmTier3 by remember { mutableStateOf(prefs.confirmTier3) }
    var retentionDays by remember { mutableStateOf(prefs.auditRetentionDays.toString()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("LLM Configuration", style = MaterialTheme.typography.titleMedium) }
        item {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                TextField(
                    value = llmProvider,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("LLM Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("ollama", "mediapipe").forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider) },
                            onClick = {
                                llmProvider = provider
                                prefs.llmProvider = provider
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        item {
            if (llmProvider == "ollama") {
                OutlinedTextField(
                    value = ollamaHost,
                    onValueChange = { ollamaHost = it; prefs.ollamaHost = it },
                    label = { Text("Ollama Host") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = ollamaModel,
                    onValueChange = { ollamaModel = it; prefs.ollamaModel = it },
                    label = { Text("Model Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            Divider()
            Spacer(Modifier.height(8.dp))
            Text("Safety", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Confirm destructive actions")
                Switch(
                    checked = confirmTier3,
                    onCheckedChange = { confirmTier3 = it; prefs.confirmTier3 = it }
                )
            }
        }
        item {
            Divider()
            Spacer(Modifier.height(8.dp))
            Text("Offline Voice", style = MaterialTheme.typography.titleMedium)
            Text(modelStatus, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                scope.launch {
                    modelStatus = "Downloading Vosk model..."
                    modelStatus = modelManager.download("https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip")
                        .fold({ "Vosk model installed" }, { "Model download failed: ${it.message}" })
                }
            }, enabled = !modelManager.isInstalled) { Text("Download English voice model") }
        }
        item {
            Divider()
            Spacer(Modifier.height(8.dp))
            Text("Audit Log", style = MaterialTheme.typography.titleMedium)
        }
        item {
            OutlinedTextField(
                value = retentionDays,
                onValueChange = {
                    retentionDays = it
                    it.toIntOrNull()?.let { days -> prefs.auditRetentionDays = days }
                },
                label = { Text("Retention (days)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = { exportLauncher.launch("aionos-audit-${System.currentTimeMillis()}.csv") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Export Audit Log")
            }
            exportStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun PluginScreen() {
    val context = LocalContext.current
    val loader = remember { PluginLoader(context) }
    var plugins by remember { mutableStateOf<List<PluginLoader.Plugin>>(emptyList()) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { plugins = loader.scanForPlugins() }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Installed Plugins", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick = { plugins = loader.scanForPlugins() }) { Text("Rescan") }
        }
        if (plugins.isEmpty()) Text("No compatible AionOS plugins found.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(plugins) { plugin ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(plugin.manifest.name, style = MaterialTheme.typography.titleMedium)
                        Text("Version ${plugin.manifest.version} · ${plugin.packageName}", style = MaterialTheme.typography.bodySmall)
                        plugin.manifest.actions.forEach { action ->
                            OutlinedButton(onClick = {
                                message = if (loader.executePluginAction(action.name, emptyMap())) {
                                    "Executed ${action.name}"
                                } else {
                                    "Plugin action failed or is unavailable"
                                }
                            }) { Text(action.name) }
                        }
                    }
                }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}
