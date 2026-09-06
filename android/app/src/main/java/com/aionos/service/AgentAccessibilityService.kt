package com.aionos.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aionos.action.AgentAction
import com.aionos.action.SafeActionExecutor
import com.aionos.audit.AuditLog
import com.aionos.parser.AccessibilityTreeParser
import com.aionos.security.EncryptedPrefs
import kotlinx.coroutines.*

/**
 * Core AccessibilityService for aionos.
 * REQUIRES: android:canPerformGestures="true" in accessibility_service_config.xml
 * for dispatchGesture() to work.
 */
class AgentAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var prefs: EncryptedPrefs
    private lateinit var auditLog: AuditLog
    private lateinit var treeParser: AccessibilityTreeParser
    lateinit var actionExecutor: SafeActionExecutor
        private set

    private var currentPackage: String = ""
    private var killSwitchRunnable: Runnable? = null
    var confirmationCallback: suspend (AgentAction) -> Boolean = { false }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        prefs = EncryptedPrefs(this)
        auditLog = AuditLog(this)
        treeParser = AccessibilityTreeParser()
        actionExecutor = SafeActionExecutor(
            service = this,
            prefs = prefs,
            auditLog = auditLog,
            treeParser = treeParser,
            onConfirmationRequired = { action -> confirmationCallback(action) }
        )

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }

        startKillSwitchPolling()

        if (prefs.isFirstRun) {
            showSetupNotification()
            prefs.isFirstRun = false
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                event.packageName?.toString()?.let { pkg ->
                    if (pkg != currentPackage) {
                        currentPackage = pkg
                        onAppChanged(pkg)
                    }
                }
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {}
        }
    }

    override fun onInterrupt() {
        prefs.isAgentEnabled = false
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        stopKillSwitchPolling()
        serviceScope.cancel()
        return super.onUnbind(intent)
    }

    fun getCurrentTree(): String = treeParser.parse(rootInActiveWindow)

    fun findNode(text: String): AccessibilityNodeInfo? =
        treeParser.findNodeByText(rootInActiveWindow, text)

    fun findNodeAt(x: Int, y: Int): AccessibilityNodeInfo? =
        treeParser.findNodeAtPoint(rootInActiveWindow, x, y)

    fun emergencyStop() {
        prefs.emergencyStop()
        stopKillSwitchPolling()
    }

    private fun startKillSwitchPolling() {
        killSwitchRunnable = object : Runnable {
            override fun run() {
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(killSwitchRunnable!!)
    }

    private fun stopKillSwitchPolling() {
        killSwitchRunnable?.let { handler.removeCallbacks(it) }
    }

    private fun onAppChanged(packageName: String) {
        serviceScope.launch {
            auditLog.record(
                action = AgentAction.Wait(0),
                success = true,
                targetApp = packageName,
                error = null
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun showSetupNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(SETUP_CHANNEL, "AionOS setup", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val intent = Intent(this, com.aionos.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            SETUP_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, SETUP_CHANNEL)
        } else {
            android.app.Notification.Builder(this)
        }
        manager.notify(
            SETUP_NOTIFICATION_ID,
            builder.setSmallIcon(com.aionos.R.drawable.ic_agent)
                .setContentTitle("Finish AionOS setup")
                .setContentText("Open AionOS to review permissions and safety settings.")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        )
    }

    companion object {
        private const val SETUP_CHANNEL = "aionos_setup"
        private const val SETUP_NOTIFICATION_ID = 100
        const val TAG = "AgentAccessibilityService"
        @Volatile
        var instance: AgentAccessibilityService? = null
            private set
    }

}
