package com.yandex.school.casheye.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.yandex.school.casheye.domain.settings.SecuritySettings
import com.yandex.school.casheye.feature.settings.presentation.AppLockScreen
import kotlinx.coroutines.launch

@Composable
internal fun AppLockGate(
    security: SecuritySettings,
    biometricsAvailable: Boolean,
    requestBiometricAuthentication: (onResult: (Boolean) -> Unit) -> Unit,
    verifyPin: suspend (CharArray) -> Boolean,
    content: @Composable () -> Unit,
) {
    val verifier = security.pinVerifier
    val sessionStartedWithoutPin = rememberSaveable { verifier == null }
    if (verifier == null) {
        content()
        return
    }

    var locked by rememberSaveable(verifier.hash) { mutableStateOf(!sessionStartedWithoutPin) }
    var biometricRequested by rememberSaveable(verifier.hash) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycle = ProcessLifecycleOwner.get().lifecycle

    DisposableEffect(lifecycle, verifier.hash) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                locked = true
                biometricRequested = false
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    if (!locked) {
        content()
        return
    }

    LaunchedEffect(locked, security.biometricsEnabled, biometricsAvailable) {
        if (locked && security.biometricsEnabled && biometricsAvailable && !biometricRequested) {
            biometricRequested = true
            requestBiometricAuthentication { succeeded ->
                if (succeeded) locked = false
            }
        }
    }
    AppLockScreen(
        biometricsEnabled = security.biometricsEnabled && biometricsAvailable,
        onPinSubmitted = { pin ->
            scope.launch {
                if (verifyPin(pin)) locked = false
            }
        },
    )
}
