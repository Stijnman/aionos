package com.aionos.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricGate(private val activity: FragmentActivity) {
    fun canAuthenticate(): Boolean = BiometricManager.from(activity).canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    ) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(onResult: (Boolean) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
            override fun onAuthenticationFailed() = Unit
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock AionOS settings")
                .setSubtitle("Authenticate to view and change protected settings")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }
}
