package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal object AppLockPinEffects

internal enum class PinAnimationState { Idle, Verifying, Success, Error }

private val PIN_ENTRY_WIDE_DURATION = 140.milliseconds
private const val PIN_ENTRY_WIDE_SCALE = 1.5f
private const val PIN_ENTRY_NARROW_SCALE = 0.85f
private val PIN_ENTRY_NARROW_DURATION = 110.milliseconds
private val PIN_ENTRY_SETTLE_DURATION = 90.milliseconds
private val PIN_ENTRY_COLOR_RESET_DURATION = 100.milliseconds
internal val PIN_ENTRY_ANIMATION_DURATION =
    PIN_ENTRY_WIDE_DURATION + PIN_ENTRY_NARROW_DURATION + PIN_ENTRY_SETTLE_DURATION
private val PIN_SUCCESS_COLOR_DURATION = 140.milliseconds
private val PIN_SUCCESS_COLLAPSE_DURATION = 350.milliseconds
private val PIN_ERROR_EXPAND_DURATION = 100.milliseconds
private val PIN_ERROR_SHRINK_DURATION = 160.milliseconds
private val PIN_ERROR_COLOR_RESET_DURATION = 150.milliseconds

internal fun CoroutineScope.launchPinEntryAnimation(
    index: Int,
    entryScaleXs: List<Animatable<Float, AnimationVector1D>>,
    cellColors: List<Animatable<Color, AnimationVector4D>>,
    enteredColor: Color,
) : Job =
    launch {
        coroutineScope {
            launch {
                entryScaleXs[index].snapTo(1f)
                entryScaleXs[index].animateTo(
                    PIN_ENTRY_WIDE_SCALE,
                    tween(PIN_ENTRY_WIDE_DURATION.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing),
                )
                entryScaleXs[index].animateTo(
                    PIN_ENTRY_NARROW_SCALE,
                    tween(PIN_ENTRY_NARROW_DURATION.inWholeMilliseconds.toInt(), easing = FastOutLinearInEasing),
                )
                entryScaleXs[index].animateTo(
                    1f,
                    tween(PIN_ENTRY_SETTLE_DURATION.inWholeMilliseconds.toInt(), easing = LinearOutSlowInEasing),
                )
            }
            launch {
                cellColors[index].animateTo(
                    enteredColor,
                    tween(PIN_ENTRY_WIDE_DURATION.inWholeMilliseconds.toInt(), easing = FastOutSlowInEasing),
                )
            }
        }
    }

internal fun CoroutineScope.launchPinCellResetAnimation(
    index: Int,
    entryScaleXs: List<Animatable<Float, AnimationVector1D>>,
    cellColors: List<Animatable<Color, AnimationVector4D>>,
    defaultColor: Color,
): Job =
    launch {
        entryScaleXs[index].snapTo(1f)
        cellColors[index].animateTo(
            defaultColor,
            tween(PIN_ENTRY_COLOR_RESET_DURATION.inWholeMilliseconds.toInt()),
        )
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
        resultProgress.snapTo(0f)
        stateChange(PinAnimationState.Verifying)
        delay(PIN_ENTRY_ANIMATION_DURATION)
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
