package com.yandex.school.casheye.feature.settings.presentation

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val PIN_LENGTH = 4

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
    useSystemInput: Boolean = true,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(enabled, useSystemInput) {
        if (enabled && useSystemInput) focusRequester.requestFocus()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(PIN_LENGTH) { index ->
                val isFilled = index < value.length || fillEmptyCells
                val targetIndicatorColor =
                    if (isError) MaterialTheme.colorScheme.error else indicatorColorForCell(index)
                val animatedIndicatorColor =
                    animateColorAsState(
                        targetValue = targetIndicatorColor,
                        animationSpec = tween(durationMillis = 150),
                        label = "pinIndicatorColor$index",
                    )
                Box(
                    modifier =
                        if (isFilled) {
                            Modifier
                                .size(16.dp)
                                .background(animatedIndicatorColor.value, CircleShape)
                        } else {
                            Modifier
                                .size(16.dp)
                                .border(2.dp, animatedIndicatorColor.value, CircleShape)
                        }.graphicsLayer {
                            scaleX = cellScaleX(index)
                            scaleY = cellScaleY(index)
                            translationX = cellTranslationX(index)
                            translationY = cellTranslationY(index)
                        }.testTag("${cellTestTagPrefix}_$index"),
                )
            }
        }
        if (useSystemInput) {
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
    }
}
