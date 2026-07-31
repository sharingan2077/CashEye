package com.yandex.school.casheye.feature.settings.presentation

import android.content.res.Configuration
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.core.designsystem.theme.CashEyeExtendedTheme
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.feature.settings.R
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.Job
import java.util.Calendar

@Composable
fun AppLockScreen(
    verifier: PinVerifier,
    biometricsEnabled: Boolean,
    onRequestBiometricAuthentication: () -> Unit,
    onPinVerificationError: () -> Unit,
    onPinVerify: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppLockViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var pin by remember { mutableStateOf("") }
    var pendingPin by remember { mutableStateOf<String?>(null) }
    var animationState by remember { mutableStateOf(PinAnimationState.Idle) }
    val animationScope = rememberCoroutineScope()
    val defaultColor = MaterialTheme.colorScheme.outlineVariant
    val enteredColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val successColor = CashEyeExtendedTheme.colors.chartIncome
    val cellColors = remember { List(PIN_DIGIT_COUNT) { Animatable(defaultColor) } }
    val entryScaleXs = remember { List(PIN_DIGIT_COUNT) { Animatable(1f) } }
    val pinEntryAnimationJobs = remember { arrayOfNulls<Job>(PIN_DIGIT_COUNT) }
    val resultProgress = remember { Animatable(0f) }
    val cellCenterDistancePx = with(LocalDensity.current) { PIN_CELL_CENTER_DISTANCE.toPx() }
    val inputEnabled = pendingPin == null && state.verification != AppLockVerificationState.Verifying
    SubmitPendingPinEffect(
        pendingPin,
        resultProgress,
        { animationState = it },
    ) { viewModel.onIntent(AppLockIntent.SubmitPin(it.toCharArray(), verifier)) }
    HandlePinVerificationResultEffect(
        state.verification,
        resultProgress,
        { animationState = it },
        onPinVerificationError,
        {
            pin = ""
            pendingPin = null
            for (index in 0 until PIN_DIGIT_COUNT) {
                pinEntryAnimationJobs[index]?.cancel()
                pinEntryAnimationJobs[index] =
                    animationScope.launchPinCellResetAnimation(
                        index,
                        entryScaleXs,
                        cellColors,
                        defaultColor,
                    )
            }
        },
        viewModel::onIntent,
        onPinVerify,
    )
    Box(
        modifier =
            modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.secondaryContainer,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, top = 48.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Text(
                appLockGreeting(),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(
                    if (pendingPin != null ||
                        state.verification == AppLockVerificationState.Verifying
                    ) {
                        R.string.app_lock_checking_hint
                    } else {
                        R.string.app_lock_pin_hint
                    },
                ),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(.18f))
            AppLockPinIndicators(
                pin,
                inputEnabled,
                animationState,
                cellColors,
                defaultColor,
                successColor,
                errorColor,
                resultProgress,
                entryScaleXs,
                cellCenterDistancePx,
            )
            Spacer(Modifier.weight(.55f))
            AppLockNumberPad(
                showFingerprint = biometricsEnabled && pin.isEmpty(),
                showBackspace = pin.isNotEmpty(),
                enabled = inputEnabled,
                onDigit = { digit ->
                    val updated = (pin + digit).take(PIN_DIGIT_COUNT)
                    if (updated.length > pin.length) {
                        val enteredIndex = pin.length
                        pinEntryAnimationJobs[enteredIndex]?.cancel()
                        pinEntryAnimationJobs[enteredIndex] =
                            animationScope.launchPinEntryAnimation(
                                enteredIndex,
                                entryScaleXs,
                                cellColors,
                                enteredColor,
                            )
                    }
                    pin = updated
                    if (updated.length == PIN_DIGIT_COUNT) pendingPin = updated
                },
                onBackspace = {
                    if (pin.isNotEmpty()) {
                        val removedIndex = pin.lastIndex
                        pin = pin.dropLast(1)
                        pinEntryAnimationJobs[removedIndex]?.cancel()
                        pinEntryAnimationJobs[removedIndex] =
                            animationScope.launchPinCellResetAnimation(
                                removedIndex,
                                entryScaleXs,
                                cellColors,
                                defaultColor,
                            )
                    }
                },
                onFingerprint = onRequestBiometricAuthentication,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun appLockGreeting(): String =
    stringResource(
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> R.string.app_lock_greeting_morning
            in 12..17 -> R.string.app_lock_greeting_afternoon
            in 18..22 -> R.string.app_lock_greeting_evening
            else -> R.string.app_lock_greeting_night
        },
    )

@Preview(name = "Light", showBackground = true, widthDp = 412, heightDp = 892, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 412, heightDp = 892, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AppLockScreenPreview() {
    CashEyeTheme(dynamicColor = false) { AppLockScreen(PinVerifier("preview", "preview"), true, {}, {}, {}) }
}
