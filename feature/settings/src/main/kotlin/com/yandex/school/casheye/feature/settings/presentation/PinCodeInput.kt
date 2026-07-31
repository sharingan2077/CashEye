package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds

private const val PIN_LENGTH = 4
private const val PIN_WIDE_SCALE = 1.5f
private const val PIN_NARROW_SCALE = 0.85f
private val PIN_INDICATOR_SIZE = 16.dp
private val PIN_INDICATOR_SLOT_SIZE = 32.dp
private val PIN_ENTRY_WIDE_DURATION = 140.milliseconds
private val PIN_ENTRY_NARROW_DURATION = 110.milliseconds
private val PIN_ENTRY_SETTLE_DURATION = 90.milliseconds

internal val PinCellScaleKey = SemanticsPropertyKey<Float>("PinCellScale")
internal var SemanticsPropertyReceiver.pinCellScale by PinCellScaleKey

@Composable
internal fun PinCodeInput(
    value: String,
    inputTestTag: String,
    cellTestTagPrefix: String,
    onValueChange: (String) -> Unit,
    onPinComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    indicatorColor: Color = MaterialTheme.colorScheme.primary,
    indicatorColorForCell: (Int) -> Color = { indicatorColor },
    fillEmptyCells: Boolean = false,
    cellScaleX: (Int) -> Float = { 1f },
    cellScaleY: (Int) -> Float = { 1f },
    cellTranslationX: (Int) -> Float = { 0f },
    cellTranslationY: (Int) -> Float = { 0f },
    animateEntry: Boolean = true,
    animateIndicatorColor: Boolean = true,
    useSystemInput: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }
    var previousValueLength by remember { mutableIntStateOf(0) }
    val entryScaleXs = remember { List(PIN_LENGTH) { Animatable(1f) } }

    LaunchedEffect(enabled, useSystemInput) {
        if (enabled && useSystemInput) focusRequester.requestFocus()
    }

    LaunchedEffect(value.length, animateEntry) {
        if (!animateEntry) return@LaunchedEffect
        val enteredIndex = value.lastIndex
        if (value.length > previousValueLength && enteredIndex >= 0) {
            entryScaleXs.forEachIndexed { index, scale ->
                if (index != enteredIndex) scale.snapTo(1f)
            }
            entryScaleXs[enteredIndex].snapTo(1f)
            entryScaleXs[enteredIndex].animateTo(
                PIN_WIDE_SCALE,
                tween(
                    durationMillis = PIN_ENTRY_WIDE_DURATION.inWholeMilliseconds.toInt(),
                    easing = FastOutSlowInEasing,
                ),
            )
            entryScaleXs[enteredIndex].animateTo(
                PIN_NARROW_SCALE,
                tween(
                    durationMillis = PIN_ENTRY_NARROW_DURATION.inWholeMilliseconds.toInt(),
                    easing = FastOutLinearInEasing,
                ),
            )
            entryScaleXs[enteredIndex].animateTo(
                1f,
                tween(
                    durationMillis = PIN_ENTRY_SETTLE_DURATION.inWholeMilliseconds.toInt(),
                    easing = LinearOutSlowInEasing,
                ),
            )
        } else if (value.length < previousValueLength) {
            for (index in value.length until previousValueLength) {
                entryScaleXs[index].snapTo(1f)
            }
        }
        previousValueLength = value.length
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            repeat(PIN_LENGTH) { index ->
                val isFilled = index < value.length || fillEmptyCells
                val entryIndicatorSize = PIN_INDICATOR_SIZE * entryScaleXs[index].value
                val cellScaleXValue = cellScaleX(index)
                val cellScaleYValue = cellScaleY(index)
                val targetIndicatorColor =
                    if (isError) MaterialTheme.colorScheme.error else indicatorColorForCell(index)
                val indicatorColor =
                    if (animateIndicatorColor) {
                        animateColorAsState(
                            targetValue = targetIndicatorColor,
                            animationSpec =
                                tween(
                                    durationMillis = PIN_ENTRY_WIDE_DURATION.inWholeMilliseconds.toInt(),
                                    easing = FastOutSlowInEasing,
                                ),
                            label = "pinIndicatorColor$index",
                        ).value
                    } else {
                        targetIndicatorColor
                    }
                Box(
                    modifier =
                        Modifier
                            .size(PIN_INDICATOR_SLOT_SIZE)
                            .testTag("${cellTestTagPrefix}_$index")
                            .graphicsLayer {
                                scaleX = cellScaleXValue
                                scaleY = cellScaleYValue
                                translationX = cellTranslationX(index)
                                translationY = cellTranslationY(index)
                            }.semantics { pinCellScale = cellScaleXValue },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            if (isFilled) {
                                Modifier
                                    .size(entryIndicatorSize)
                                    .background(indicatorColor, CircleShape)
                            } else {
                                Modifier
                                    .size(entryIndicatorSize)
                                    .border(2.dp, indicatorColor, CircleShape)
                            },
                    )
                }
            }
        }
        if (useSystemInput) {
            PinCodeSystemInput(
                value = value,
                inputTestTag = inputTestTag,
                focusRequester = focusRequester,
                onValueChange = onValueChange,
                onPinComplete = onPinComplete,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun PinCodeSystemInput(
    value: String,
    inputTestTag: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onPinComplete: (String) -> Unit,
    enabled: Boolean,
) {
    BasicTextField(
        value = value,
        onValueChange = { input ->
            val digits = input.filter(Char::isDigit).take(PIN_LENGTH)
            onValueChange(digits)
            if (digits.length == PIN_LENGTH) {
                onPinComplete(digits)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .alpha(0f)
                .focusRequester(focusRequester)
                .testTag(inputTestTag),
        enabled = enabled,
    )
}
