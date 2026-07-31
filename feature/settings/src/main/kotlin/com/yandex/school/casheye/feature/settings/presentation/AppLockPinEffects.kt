package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal object AppLockPinEffects

internal enum class PinAnimationState { Idle, Verifying, Success, Error }

private val PIN_ENTRY_WIDE_DURATION = 140.milliseconds
private val PIN_ENTRY_COLOR_RESET_DURATION = 100.milliseconds
private val PIN_LAST_ENTRY_SETTLE_DURATION = 260.milliseconds
private val PIN_SUCCESS_COLOR_DURATION = 140.milliseconds
private val PIN_SUCCESS_COLLAPSE_DURATION = 350.milliseconds
private val PIN_ERROR_EXPAND_DURATION = 100.milliseconds
private val PIN_ERROR_SHRINK_DURATION = 160.milliseconds
private val PIN_ERROR_COLOR_RESET_DURATION = 150.milliseconds

@Composable
internal fun PinEntryColorEffect(
    pin: String,
    previousPinLength: Int,
    cellColors: List<Animatable<Color, AnimationVector4D>>,
    enteredColor: Color,
    defaultColor: Color,
    onPreviousPinLengthChange: (Int) -> Unit,
) {
    val callback by rememberUpdatedState(onPreviousPinLengthChange)
    LaunchedEffect(pin.length) {
        val index = pin.lastIndex
        if (pin.length > previousPinLength && index >= 0) {
            cellColors[index].animateTo(
                enteredColor,
                tween(PIN_ENTRY_WIDE_DURATION.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing),
            )
        } else if (pin.length < previousPinLength) {
            coroutineScope {
                for (cellIndex in pin.length until previousPinLength) {
                    launch {
                        cellColors[cellIndex].animateTo(
                            defaultColor,
                            tween(PIN_ENTRY_COLOR_RESET_DURATION.inWholeMilliseconds.toInt()),
                        )
                    }
                }
            }
        }
        callback(pin.length)
    }
}

@Composable
internal fun SubmitPendingPinEffect(
    pendingPin: String?,
    resultProgress: Animatable<Float, AnimationVector1D>,
    onAnimationStateChange: (PinAnimationState) -> Unit,
    onSubmitPin: (String) -> Unit,
) {
    val stateChange by rememberUpdatedState(onAnimationStateChange)
    val submit by rememberUpdatedState(onSubmitPin)
    LaunchedEffect(pendingPin) {
        val pin =
            pendingPin
                ?: return@LaunchedEffect
        ; resultProgress.snapTo(0f)
        stateChange(PinAnimationState.Verifying)
        delay(
            PIN_LAST_ENTRY_SETTLE_DURATION,
        )
        submit(pin)
    }
}

@Composable
internal fun HandlePinVerificationResultEffect(
    verification: AppLockVerificationState,
    resultProgress: Animatable<Float, AnimationVector1D>,
    onAnimationStateChange: (PinAnimationState) -> Unit,
    onErrorHapticFeedback: () -> Unit,
    onResetPin: () -> Unit,
    onAnimationFinish: (AppLockIntent) -> Unit,
    onPinVerify: () -> Unit,
) {
    val stateChange by rememberUpdatedState(onAnimationStateChange)
    val haptic by rememberUpdatedState(onErrorHapticFeedback)
    val reset by rememberUpdatedState(onResetPin)
    val finish by rememberUpdatedState(onAnimationFinish)
    val verified by rememberUpdatedState(onPinVerify)
    LaunchedEffect(verification) {
        when (verification) {
            AppLockVerificationState.Success -> {
                stateChange(PinAnimationState.Success)
                delay(PIN_SUCCESS_COLOR_DURATION)
                resultProgress.animateTo(
                    1f,
                    tween(PIN_SUCCESS_COLLAPSE_DURATION.inWholeMilliseconds.toInt()),
                )
                finish(AppLockIntent.SuccessAnimationFinished)
                verified()
            }

            AppLockVerificationState.Error -> {
                stateChange(PinAnimationState.Error)
                haptic()
                resultProgress.animateTo(
                    1f,
                    tween(PIN_ERROR_EXPAND_DURATION.inWholeMilliseconds.toInt()),
                )
                resultProgress.animateTo(0f, tween(PIN_ERROR_SHRINK_DURATION.inWholeMilliseconds.toInt()))
                stateChange(
                    PinAnimationState.Idle,
                )
                delay(PIN_ERROR_COLOR_RESET_DURATION)
                reset()
                finish(AppLockIntent.ErrorAnimationFinished)
            }

            AppLockVerificationState.Idle, AppLockVerificationState.Verifying -> {}
        }
    }
}
