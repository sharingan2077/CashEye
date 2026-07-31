package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal const val PIN_DIGIT_COUNT = 4
internal val PIN_CELL_CENTER_DISTANCE = 32.dp
private const val PIN_ERROR_PULSE_SCALE = .45f

@Composable
internal fun AppLockPinIndicators(
    pin: String,
    enabled: Boolean,
    animationState: PinAnimationState,
    cellColors: List<Animatable<Color, AnimationVector4D>>,
    defaultColor: Color,
    successColor: Color,
    errorColor: Color,
    resultProgress: Animatable<Float, AnimationVector1D>,
    cellCenterDistancePx: Float,
    modifier: Modifier = Modifier,
) {
    val indicatorColor =
        when (animationState) {
            PinAnimationState.Idle, PinAnimationState.Verifying -> defaultColor
            PinAnimationState.Success -> successColor
            PinAnimationState.Error -> errorColor
        }
    val resultScale =
        when (animationState) {
            PinAnimationState.Success -> {
                1f -
                    resultProgress.value
            }

            PinAnimationState.Error -> {
                1f + PIN_ERROR_PULSE_SCALE * resultProgress.value
            }

            else -> {
                1f
            }
        }
    ; PinCodeInput(
        value = pin,
        inputTestTag = "app_lock_pin_input",
        cellTestTagPrefix = "app_lock_pin_cell",
        onValueChange = {
        },
        onPinComplete = {
        },
        modifier = modifier,
        enabled = enabled,
        indicatorColor = indicatorColor,
        indicatorColorForCell = { index ->
            when (animationState) {
                PinAnimationState.Success -> successColor
                PinAnimationState.Error -> errorColor
                else -> cellColors[index].value
            }
        },
        fillEmptyCells = true,
        cellScaleX = { resultScale },
        cellScaleY = { resultScale },
        cellTranslationX = { index ->
            if (animationState ==
                PinAnimationState.Success
            ) {
                (PIN_DIGIT_COUNT / 2f - .5f - index) * cellCenterDistancePx * resultProgress.value
            } else {
                0f
            }
        },
        animateIndicatorColor = false,
        useSystemInput = false,
    )
}
