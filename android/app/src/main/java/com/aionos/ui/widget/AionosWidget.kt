package com.aionos.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aionos.MainActivity
import com.aionos.R

/**
 * AionOS App Widget for quick access to agent commands.
 * Displays agent status and allows quick command input.
 */
class AionosWidget : AppWidgetProvider() {
    
    companion object {
        const val ACTION_QUICK_COMMAND = "com.aionos.action.QUICK_COMMAND"
        const val EXTRA_COMMAND = "extra_command"
        
        // Quick command presets
        val QUICK_COMMANDS = listOf(
            "Open Settings" to "open settings",
            "Go Back" to "go back",
            "Scroll Down" to "scroll down",
            "Scroll Up" to "scroll up"
        )
    }
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update all widgets
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }
    
    override fun onEnabled(context: Context) {
        // Called when first widget is placed
        super.onEnabled(context)
    }
    
    override fun onDisabled(context: Context) {
        // Called when last widget is removed
        super.onDisabled(context)
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_QUICK_COMMAND -> {
                val command = intent.getStringExtra(EXTRA_COMMAND) ?: return
                val widgetIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("widget_command", command)
                }
                context.startActivity(widgetIntent)
            }
        }
        super.onReceive(context, intent)
    }
    
    /**
     * Update a single widget instance.
     */
    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_aionos)
        
        // Set up quick command buttons
        QUICK_COMMANDS.forEachIndexed { index, (label, command) ->
            val buttonId = context.resources.getIdentifier("quick_command_$index", "id", context.packageName)
            if (buttonId != 0) {
                views.setTextViewText(buttonId, label)
                views.setOnClickPendingIntent(
                    buttonId,
                    createQuickCommandPendingIntent(context, command, appWidgetId)
                )
            }
        }
        
        // Set up main open button
        val openIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.widget_open_app, openIntent)
        
        // Update widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    /**
     * Create a pending intent for quick commands.
     */
    private fun createQuickCommandPendingIntent(
        context: Context,
        command: String,
        appWidgetId: Int
    ): PendingIntent {
        val intent = Intent(context, AionosWidget::class.java).apply {
            action = ACTION_QUICK_COMMAND
            putExtra(EXTRA_COMMAND, command)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        return PendingIntent.getBroadcast(
            context, appWidgetId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
