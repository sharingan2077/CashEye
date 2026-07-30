package com.yandex.school.casheye.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.yandex.school.casheye.domain.settings.SecuritySettings
import com.yandex.school.casheye.feature.settings.presentation.AppLockScreen

@Composable
internal fun AppLockGate(
    security: SecuritySettings,
    biometricsAvailable: Boolean,
    requestBiometricAuthentication: (onResult: (Boolean) -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    val movableContent = remember { movableContentOf { currentContent() } }
    val currentBiometricRequest by rememberUpdatedState(requestBiometricAuthentication)
    val verifier = security.pinVerifier
    val sessionStartedWithoutPin = rememberSaveable { verifier == null }
    if (verifier == null) {
        movableContent()
        return
    }

    var locked by rememberSaveable(verifier.hash) { mutableStateOf(!sessionStartedWithoutPin) }
    var biometricRequested by rememberSaveable(verifier.hash) { mutableStateOf(false) }
    val biometricsEnabled = security.biometricsEnabled && biometricsAvailable
    val lifecycle = ProcessLifecycleOwner.get().lifecycle

    DisposableEffect(lifecycle, verifier.hash) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    locked = true
                    biometricRequested = false
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (!locked) {
        movableContent()
        return
    }

    val requestBiometricAuthentication = {
        if (locked && biometricsEnabled) {
            biometricRequested = true
            currentBiometricRequest { succeeded ->
                if (succeeded) locked = false
            }
        }
    }
    LaunchedEffect(locked, biometricsEnabled) {
        if (
            shouldRequestBiometricAuthentication(
                locked,
                biometricsEnabled,
                biometricRequested,
            )
        ) {
            requestBiometricAuthentication()
        }
    }
    AppLockScreen(
        verifier = verifier,
        biometricsEnabled = biometricsEnabled,
        onRequestBiometricAuthentication = requestBiometricAuthentication,
        onPinVerified = { locked = false },
    )
}

private fun shouldRequestBiometricAuthentication(
    locked: Boolean,
    biometricsEnabled: Boolean,
    biometricRequested: Boolean,
): Boolean = locked && biometricsEnabled && !biometricRequested
