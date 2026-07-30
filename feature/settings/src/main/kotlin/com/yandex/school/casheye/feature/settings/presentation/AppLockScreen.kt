package com.yandex.school.casheye.feature.settings.presentation

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.theme.CashEyeExtendedTheme
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.settings.R
import kotlinx.coroutines.delay
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

private const val PIN_DIGIT_COUNT = 4
private const val PIN_WIDE_SCALE = 2.75f
private const val PIN_ERROR_PULSE_SCALE = 0.45f
private val PIN_ENTRY_NARROW_DURATION = 260.milliseconds
private val PIN_LAST_ENTRY_SETTLE_DURATION = 260.milliseconds
private val PIN_SUCCESS_COLOR_DURATION = 140.milliseconds
private val PIN_SUCCESS_COLLAPSE_DURATION = 350.milliseconds
private val PIN_ERROR_EXPAND_DURATION = 100.milliseconds
private val PIN_ERROR_SHRINK_DURATION = 160.milliseconds
private val PIN_ERROR_COLOR_RESET_DURATION = 150.milliseconds
private val PIN_CELL_CENTER_DISTANCE = 32.dp

private enum class PinAnimationState {
    Idle,
    Verifying,
    Success,
    Error,
}

@Composable
fun AppLockScreen(
    biometricsEnabled: Boolean,
    onRequestBiometricAuthentication: () -> Unit,
    onVerifyPin: suspend (CharArray) -> Boolean,
    onPinVerified: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pin by remember { mutableStateOf("") }
    var pendingPin by remember { mutableStateOf<String?>(null) }
    var isVerifying by remember { mutableStateOf(false) }
    var pinAnimationState by remember { mutableStateOf(PinAnimationState.Idle) }
    var previousPinLength by remember { mutableIntStateOf(0) }
    val cellScales = remember { List(PIN_DIGIT_COUNT) { Animatable(1f) } }
    val resultProgress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val cellCenterDistancePx = with(density) { PIN_CELL_CENTER_DISTANCE.toPx() }
    val isPinInputEnabled = pendingPin == null && !isVerifying

    LaunchedEffect(pin.length) {
        val enteredIndex = pin.lastIndex
        if (pin.length > previousPinLength && enteredIndex >= 0) {
            cellScales.forEachIndexed { index, scale ->
                if (index != enteredIndex) scale.snapTo(1f)
            }
            cellScales[enteredIndex].snapTo(PIN_WIDE_SCALE)
            cellScales[enteredIndex].animateTo(
                1f,
                tween(PIN_ENTRY_NARROW_DURATION.inWholeMilliseconds.toInt()),
            )
        } else if (pin.length < previousPinLength) {
            cellScales[pin.length].snapTo(1f)
        }
        previousPinLength = pin.length
    }

    LaunchedEffect(pendingPin) {
        val submittedPin = pendingPin ?: return@LaunchedEffect
        isVerifying = true
        resultProgress.snapTo(0f)
        pinAnimationState = PinAnimationState.Verifying
        delay(PIN_LAST_ENTRY_SETTLE_DURATION)
        val isPinCorrect = onVerifyPin(submittedPin.toCharArray())
        if (isPinCorrect) {
            pinAnimationState = PinAnimationState.Success
            delay(PIN_SUCCESS_COLOR_DURATION)
            resultProgress.animateTo(
                1f,
                tween(PIN_SUCCESS_COLLAPSE_DURATION.inWholeMilliseconds.toInt()),
            )
            onPinVerified()
        } else {
            pinAnimationState = PinAnimationState.Error
            resultProgress.animateTo(
                1f,
                tween(PIN_ERROR_EXPAND_DURATION.inWholeMilliseconds.toInt()),
            )
            resultProgress.animateTo(
                0f,
                tween(PIN_ERROR_SHRINK_DURATION.inWholeMilliseconds.toInt()),
            )
            pinAnimationState = PinAnimationState.Idle
            delay(PIN_ERROR_COLOR_RESET_DURATION)
            pin = ""
            isVerifying = false
            pendingPin = null
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    ),
                ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp, end = 32.dp, top = 48.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                text = appLockGreeting(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text =
                    androidx.compose.ui.res.stringResource(
                        if (pendingPin != null || isVerifying) {
                            R.string.app_lock_checking_hint
                        } else {
                            R.string.app_lock_pin_hint
                        },
                    ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(0.18f))
            val defaultPinIndicatorColor = MaterialTheme.colorScheme.outlineVariant
            val enteredPinIndicatorColor = MaterialTheme.colorScheme.primary
            val errorPinIndicatorColor = MaterialTheme.colorScheme.error
            val successPinIndicatorColor = CashEyeExtendedTheme.colors.chartIncome
            PinCodeInput(
                value = pin,
                inputTestTag = "app_lock_pin_input",
                onValueChange = {},
                onPinComplete = {},
                cellTestTagPrefix = "app_lock_pin_cell",
                enabled = isPinInputEnabled,
                indicatorColor =
                    when (pinAnimationState) {
                        PinAnimationState.Idle, PinAnimationState.Verifying ->
                            defaultPinIndicatorColor
                        PinAnimationState.Success -> successPinIndicatorColor
                        PinAnimationState.Error -> errorPinIndicatorColor
                    },
                indicatorColorForCell = { index ->
                    when (pinAnimationState) {
                        PinAnimationState.Success -> successPinIndicatorColor
                        PinAnimationState.Error -> errorPinIndicatorColor
                        else ->
                            if (index < pin.length) {
                                enteredPinIndicatorColor
                            } else {
                                defaultPinIndicatorColor
                            }
                    }
                },
                fillEmptyCells = true,
                cellScaleX = { index ->
                    val resultScale =
                        when (pinAnimationState) {
                            PinAnimationState.Success -> 1f - resultProgress.value
                            PinAnimationState.Error -> 1f + PIN_ERROR_PULSE_SCALE * resultProgress.value
                            else -> 1f
                        }
                    cellScales[index].value * resultScale
                },
                cellScaleY = { index ->
                    val resultScale =
                        when (pinAnimationState) {
                        PinAnimationState.Success -> 1f - resultProgress.value
                        PinAnimationState.Error -> 1f + PIN_ERROR_PULSE_SCALE * resultProgress.value
                        else -> 1f
                        }
                    cellScales[index].value * resultScale
                },
                cellTranslationX = { index ->
                    if (pinAnimationState == PinAnimationState.Success) {
                        (PIN_DIGIT_COUNT / 2f - 0.5f - index) *
                            cellCenterDistancePx * resultProgress.value
                    } else {
                        0f
                    }
                },
                useSystemInput = false,
            )
            Spacer(Modifier.weight(0.55f))
            AppLockNumberPad(
                showFingerprint = biometricsEnabled && pin.isEmpty(),
                showBackspace = pin.isNotEmpty(),
                enabled = isPinInputEnabled,
                onDigit = { digit ->
                    val updatedPin = (pin + digit).take(PIN_DIGIT_COUNT)
                    pin = updatedPin
                    if (updatedPin.length == PIN_DIGIT_COUNT) pendingPin = updatedPin
                },
                onBackspace = { pin = pin.dropLast(1) },
                onFingerprint = onRequestBiometricAuthentication,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun appLockGreeting(): String =
    androidx.compose.ui.res.stringResource(
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.app_lock_greeting_morning
            in 12..17 -> R.string.app_lock_greeting_afternoon
            in 18..22 -> R.string.app_lock_greeting_evening
            else -> R.string.app_lock_greeting_night
        },
    )

@Preview(
    name = "Light",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AppLockScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        AppLockScreen(
            biometricsEnabled = true,
            onRequestBiometricAuthentication = {},
            onVerifyPin = { false },
            onPinVerified = {},
        )
    }
}
