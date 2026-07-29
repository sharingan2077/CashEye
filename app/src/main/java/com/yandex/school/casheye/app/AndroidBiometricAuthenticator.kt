package com.yandex.school.casheye.app

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class AndroidBiometricAuthenticator(
    private val activity: FragmentActivity,
) {
    fun isAvailable(): Boolean =
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate(onResult: (Boolean) -> Unit) {
        if (!isAvailable()) {
            onResult(false)
            return
        }
        BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onResult(true)
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence,
                ) {
                    onResult(false)
                }

                override fun onAuthenticationFailed() = Unit
            },
        ).authenticate(
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle("CashEye")
                .setNegativeButtonText(
                    activity.getString(com.yandex.school.casheye.feature.settings.R.string.app_lock_use_pin),
                ).setAllowedAuthenticators(AUTHENTICATORS)
                .build(),
        )
    }

    private companion object {
        const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG
    }
}
