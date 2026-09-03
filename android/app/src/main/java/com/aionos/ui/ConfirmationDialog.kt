package com.aionos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aionos.action.AgentAction

@Composable
fun ConfirmationDialog(
    action: AgentAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (action) {
        is AgentAction.Type -> if (action.isPasswordField) "Type into password field?" else "Confirm text input"
        is AgentAction.OpenApp -> "Open ${action.packageName}?"
        else -> "Confirm action"
    }
    val description = when (action) {
        is AgentAction.Type -> if (action.isPasswordField) {
            "The agent wants to type into a password field. This is a sensitive operation."
        } else {
            "The agent wants to type: "${action.text.take(50)}${if (action.text.length > 50) "..." else ""}""
        }
        is AgentAction.OpenApp -> "The agent wants to open an application."
        else -> "The agent wants to perform a potentially sensitive action."
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(description)
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        text = "Action: ${action.javaClass.simpleName}",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Allow Once")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Deny") }
        }
    )
}
