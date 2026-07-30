package com.yandex.school.casheye.feature.settings.presentation

import android.content.res.Configuration
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.AnimationVector4D
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.core.designsystem.theme.CashEyeExtendedTheme
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.feature.settings.R
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

private const val PIN_DIGIT_COUNT = 4
private const val PIN_ERROR_PULSE_SCALE = 0.45f
private val PIN_ENTRY_WIDE_DURATION = 140.milliseconds
private val PIN_ENTRY_COLOR_RESET_DURATION = 100.milliseconds
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
    verifier: PinVerifier,
    biometricsEnabled: Boolean,
    onRequestBiometricAuthentication: () -> Unit,
    onPinVerificationError: () -> Unit,
    onPinVerified: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLockViewModel = metroViewModel(),
) {
    val appLockState by viewModel.state.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var pendingPin by remember { mutableStateOf<String?>(null) }
    var pinAnimationState by remember { mutableStateOf(PinAnimationState.Idle) }
    var previousPinLength by remember { mutableIntStateOf(0) }
    val defaultPinIndicatorColor = MaterialTheme.colorScheme.outlineVariant
    val enteredPinIndicatorColor = MaterialTheme.colorScheme.primary
    val errorPinIndicatorColor = MaterialTheme.colorScheme.error
    val successPinIndicatorColor = CashEyeExtendedTheme.colors.chartIncome
    val cellColors =
        remember {
            List(PIN_DIGIT_COUNT) {
                Animatable(defaultPinIndicatorColor)
            }
        }
    val resultProgress = remember { Animatable(0f) }
    val density = LocalDensity.current
    val cellCenterDistancePx = with(density) { PIN_CELL_CENTER_DISTANCE.toPx() }
    val isPinInputEnabled = pendingPin == null && appLockState.verification != AppLockVerificationState.Verifying

    PinEntryColorEffect(
        pin = pin,
        previousPinLength = previousPinLength,
        cellColors = cellColors,
        enteredColor = enteredPinIndicatorColor,
        defaultColor = defaultPinIndicatorColor,
        onPreviousPinLengthChange = { previousPinLength = it },
    )
    SubmitPendingPinEffect(
        pendingPin = pendingPin,
        resultProgress = resultProgress,
        onAnimationStateChange = { pinAnimationState = it },
        onSubmitPin = { submittedPin ->
            viewModel.onIntent(AppLockIntent.SubmitPin(submittedPin.toCharArray(), verifier))
        },
    )
    HandlePinVerificationResultEffect(
        verification = appLockState.verification,
        resultProgress = resultProgress,
        onAnimationStateChange = { pinAnimationState = it },
        onErrorHapticFeedback = onPinVerificationError,
        onResetPin = {
            pin = ""
            pendingPin = null
        },
        onAnimationFinished = viewModel::onIntent,
        onPinVerified = onPinVerified,
    )

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
                        if (pendingPin != null || appLockState.verification == AppLockVerificationState.Verifying) {
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
            AppLockPinIndicators(
                pin = pin,
                enabled = isPinInputEnabled,
                animationState = pinAnimationState,
                cellColors = cellColors,
                defaultColor = defaultPinIndicatorColor,
                successColor = successPinIndicatorColor,
                errorColor = errorPinIndicatorColor,
                resultProgress = resultProgress,
                cellCenterDistancePx = cellCenterDistancePx,
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
private fun PinEntryColorEffect(
    pin: String,
    previousPinLength: Int,
    cellColors: List<Animatable<Color, AnimationVector4D>>,
    enteredColor: Color,
    defaultColor: Color,
    onPreviousPinLengthChange: (Int) -> Unit,
) {
    LaunchedEffect(pin.length) {
        val enteredIndex = pin.lastIndex
        if (pin.length > previousPinLength && enteredIndex >= 0) {
            cellColors[enteredIndex].animateTo(
                enteredColor,
                tween(
                    durationMillis = PIN_ENTRY_WIDE_DURATION.inWholeMilliseconds.toInt(),
                    easing = FastOutSlowInEasing,
                ),
            )
        } else if (pin.length < previousPinLength) {
            coroutineScope {
                (pin.length until previousPinLength).forEach { index ->
                    launch {
                        cellColors[index].animateTo(
                            defaultColor,
                            tween(PIN_ENTRY_COLOR_RESET_DURATION.inWholeMilliseconds.toInt()),
                        )
                    }
                }
            }
        }
        onPreviousPinLengthChange(pin.length)
    }
}

@Composable
private fun SubmitPendingPinEffect(
    pendingPin: String?,
    resultProgress: Animatable<Float, AnimationVector1D>,
    onAnimationStateChange: (PinAnimationState) -> Unit,
    onSubmitPin: (String) -> Unit,
) {
    LaunchedEffect(pendingPin) {
        val submittedPin = pendingPin ?: return@LaunchedEffect
        resultProgress.snapTo(0f)
        onAnimationStateChange(PinAnimationState.Verifying)
        delay(PIN_LAST_ENTRY_SETTLE_DURATION)
        onSubmitPin(submittedPin)
    }
}

@Composable
private fun HandlePinVerificationResultEffect(
    verification: AppLockVerificationState,
    resultProgress: Animatable<Float, AnimationVector1D>,
    onAnimationStateChange: (PinAnimationState) -> Unit,
    onErrorHapticFeedback: () -> Unit,
    onResetPin: () -> Unit,
    onAnimationFinished: (AppLockIntent) -> Unit,
    onPinVerified: () -> Unit,
) {
    LaunchedEffect(verification) {
        when (verification) {
            AppLockVerificationState.Success -> {
                onAnimationStateChange(PinAnimationState.Success)
                delay(PIN_SUCCESS_COLOR_DURATION)
                resultProgress.animateTo(
                    1f,
                    tween(PIN_SUCCESS_COLLAPSE_DURATION.inWholeMilliseconds.toInt()),
                )
                onAnimationFinished(AppLockIntent.SuccessAnimationFinished)
                onPinVerified()
            }

            AppLockVerificationState.Error -> {
                onAnimationStateChange(PinAnimationState.Error)
                onErrorHapticFeedback()
                resultProgress.animateTo(
                    1f,
                    tween(PIN_ERROR_EXPAND_DURATION.inWholeMilliseconds.toInt()),
                )
                resultProgress.animateTo(
                    0f,
                    tween(PIN_ERROR_SHRINK_DURATION.inWholeMilliseconds.toInt()),
                )
                onAnimationStateChange(PinAnimationState.Idle)
                delay(PIN_ERROR_COLOR_RESET_DURATION)
                onResetPin()
                onAnimationFinished(AppLockIntent.ErrorAnimationFinished)
            }

            AppLockVerificationState.Idle, AppLockVerificationState.Verifying -> {
                Unit
            }
        }
    }
}

@Composable
private fun AppLockPinIndicators(
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
            PinAnimationState.Success -> 1f - resultProgress.value
            PinAnimationState.Error -> 1f + PIN_ERROR_PULSE_SCALE * resultProgress.value
            else -> 1f
        }

    PinCodeInput(
        value = pin,
        inputTestTag = "app_lock_pin_input",
        cellTestTagPrefix = "app_lock_pin_cell",
        onValueChange = {},
        onPinComplete = {},
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
            if (animationState == PinAnimationState.Success) {
                (PIN_DIGIT_COUNT / 2f - 0.5f - index) * cellCenterDistancePx * resultProgress.value
            } else {
                0f
            }
        },
        animateIndicatorColor = false,
        useSystemInput = false,
    )
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
            verifier = PinVerifier("preview", "preview"),
            biometricsEnabled = true,
            onRequestBiometricAuthentication = {},
            onPinVerificationError = {},
            onPinVerified = {},
        )
    }
}
