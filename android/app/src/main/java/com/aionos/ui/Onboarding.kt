package com.aionos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingDialog(onFinished: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Welcome to AionOS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("AionOS turns your natural-language commands into Android UI actions on your device.")
                Text("Privacy first: screen data stays on-device. Ollama is the default provider and connects only to the host you configure.")
                Text("Safety tiers are enforced: routine actions may run automatically, destructive actions require confirmation, and blocked actions never run.")
                Text("You can enable Accessibility, microphone, overlay, and screenshot access later from the dashboard when needed.")
            }
        },
        confirmButton = {
            Button(onClick = onFinished, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text("Continue")
            }
        }
    )
}
