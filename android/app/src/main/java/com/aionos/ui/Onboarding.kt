package com.aionos.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Enhanced onboarding flow with multiple steps.
 * Guides users through setup and explains key features.
 */
@Composable
fun OnboardingDialog(onFinished: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    val steps = listOf(
        OnboardingStep(
            title = "Welcome to AionOS",
            description = "AionOS is a local-first AI agent for Android that turns your natural language commands into actions on your device.",
            icon = Icons.Default.Check,
            isFinal = false
        ),
        OnboardingStep(
            title = "Privacy First",
            description = "Your screen data stays on your device. Ollama is the default provider and connects only to the host you configure on your LAN.",
            icon = Icons.Default.Check,
            isFinal = false
        ),
        OnboardingStep(
            title = "Safety Features",
            description = "Safety tiers are enforced: routine actions auto-execute, destructive actions require confirmation, and blocked actions never run.",
            icon = Icons.Default.Check,
            isFinal = false
        ),
        OnboardingStep(
            title = "Get Started",
            description = "Enable Accessibility Service to allow AionOS to control your device. You can also enable voice input, overlay bubble, and screenshot access from settings.",
            icon = Icons.Default.Check,
            isFinal = true
        )
    )
    
    val currentStepData = steps[currentStep]
    
    AlertDialog(
        onDismissRequest = {},
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(currentStepData.title)
                LinearProgressIndicator(
                    progress = { (currentStep + 1).toFloat() / steps.size },
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 8.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    currentStepData.icon,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    currentStepData.description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                
                if (currentStep > 0) {
                    Text(
                        "Step ${currentStep + 1} of ${steps.size}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentStep > 0) {
                    IconButton(onClick = { currentStep-- }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Back")
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (currentStep < steps.size - 1) {
                            currentStep++
                        } else {
                            onFinished()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                ) {
                    if (currentStep < steps.size - 1) {
                        Text("Next")
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    } else {
                        Text("Get Started")
                    }
                }
            }
        }
    )
}

/**
 * Data class for onboarding steps.
 */
private data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isFinal: Boolean
)
