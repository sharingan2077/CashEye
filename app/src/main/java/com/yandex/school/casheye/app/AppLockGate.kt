package com.yandex.school.casheye.app

import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.yandex.school.casheye.domain.settings.SecuritySettings
import com.yandex.school.casheye.feature.settings.presentation.AppLockScreen
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

internal val APP_LOCK_BACKGROUND_GRACE_PERIOD = 5.minutes
private val APP_LOCK_PIN_ERROR_VIBRATION_DURATION = 50.milliseconds

/**
 * Keeps authentication state over Activity recreation, but not process death.
 */
private object AppLockSessionState {
    var isLocked: Boolean? = null
}

@Composable
internal fun AppLockGate(
    security: SecuritySettings,
    biometricsAvailable: Boolean,
    requestBiometricAuthentication: (onResult: (Boolean) -> Unit) -> Unit,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val vibrator = remember(context) { context.getSystemService(Vibrator::class.java) }
    val verifier = security.pinVerifier
    var locked by remember {
        mutableStateOf(AppLockSessionState.isLocked ?: (verifier != null))
    }
    SideEffect { AppLockSessionState.isLocked = locked }

    var biometricRequested by remember(verifier?.hash) { mutableStateOf(false) }
    var backgroundedAtElapsedRealtime by remember(verifier?.hash) { mutableStateOf<Long?>(null) }
    val biometricsEnabled = security.biometricsEnabled && biometricsAvailable
    val lifecycle = ProcessLifecycleOwner.get().lifecycle
    var isAppInForeground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(lifecycle, verifier?.hash) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        isAppInForeground = true
                        if (
                            verifier != null &&
                            shouldLockAfterBackground(
                                backgroundedAtElapsedRealtime = backgroundedAtElapsedRealtime,
                                currentElapsedRealtime = SystemClock.elapsedRealtime(),
                            )
                        ) {
                            locked = true
                        }
                        backgroundedAtElapsedRealtime = null
                    }

                    Lifecycle.Event.ON_STOP -> {
                        isAppInForeground = false
                        backgroundedAtElapsedRealtime = SystemClock.elapsedRealtime()
                        biometricRequested = false
                    }

                    else -> {
                        Unit
                    }
                }
            }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    val requestBiometric = {
        if (verifier != null && locked && biometricsEnabled) {
            biometricRequested = true
            requestBiometricAuthentication { succeeded ->
                if (succeeded) locked = false
            }
        }
    }
    LaunchedEffect(locked, biometricsEnabled, biometricRequested, isAppInForeground) {
        if (
            shouldRequestBiometricAuthentication(
                locked = verifier != null && locked,
                biometricsEnabled = biometricsEnabled,
                biometricRequested = biometricRequested,
                isAppInForeground = isAppInForeground,
            )
        ) {
            requestBiometric()
        }
    }
    if (verifier != null && locked) {
        AppLockScreen(
            verifier = verifier,
            biometricsEnabled = biometricsEnabled,
            onRequestBiometricAuthentication = requestBiometric,
            onPinVerificationError = {
                if (vibrator?.hasVibrator() == true) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            APP_LOCK_PIN_ERROR_VIBRATION_DURATION.inWholeMilliseconds,
                            VibrationEffect.DEFAULT_AMPLITUDE,
                        ),
                    )
                }
            },
            onPinVerified = { locked = false },
        )
    } else {
        content()
    }
}

private fun shouldRequestBiometricAuthentication(
    locked: Boolean,
    biometricsEnabled: Boolean,
    biometricRequested: Boolean,
    isAppInForeground: Boolean,
): Boolean = locked && biometricsEnabled && !biometricRequested && isAppInForeground

internal fun shouldLockAfterBackground(
    backgroundedAtElapsedRealtime: Long?,
    currentElapsedRealtime: Long,
): Boolean =
    backgroundedAtElapsedRealtime != null &&
        currentElapsedRealtime - backgroundedAtElapsedRealtime >= APP_LOCK_BACKGROUND_GRACE_PERIOD.inWholeMilliseconds
