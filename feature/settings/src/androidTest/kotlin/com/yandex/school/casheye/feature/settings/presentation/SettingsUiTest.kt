package com.yandex.school.casheye.feature.settings.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.domain.settings.AppLanguage
import com.yandex.school.casheye.domain.settings.AppSettings
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.domain.settings.SettingsRepository
import com.yandex.school.casheye.domain.settings.ThemeMode
import com.yandex.school.casheye.domain.settings.VerifyPinUseCase
import com.yandex.school.casheye.feature.settings.R
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

class SettingsUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectingThemeOptionsUpdatesState() {
        var selectedMode by mutableStateOf(ThemeMode.LIGHT)
        composeRule.setContent {
            CashEyeTheme(darkTheme = false, dynamicColor = false) {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        ThemeOption(
                            label = mode.name,
                            painter = painterResource(R.drawable.ic_settings_moon),
                            mode = mode,
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                        )
                    }
                }
            }
        }

        ThemeMode.entries.forEach { mode ->
            composeRule.onNodeWithTag("settings_theme_${mode.name.lowercase()}").performClick()

            composeRule.runOnIdle {
                assertEquals(mode, selectedMode)
            }
        }
    }

    @Test
    fun pinKeypadSubmitsFourDigits() {
        val submittedPins = mutableListOf<String>()
        val viewModel =
            AppLockViewModel(
                VerifyPinUseCase(
                    object : SettingsRepository {
                        override fun observe(): Flow<AppSettings> = emptyFlow()

                        override suspend fun setThemeMode(mode: ThemeMode) = Unit

                        override suspend fun setLanguage(language: AppLanguage) = Unit

                        override suspend fun setPin(pin: CharArray?) = Unit

                        override suspend fun verifyPin(
                            pin: CharArray,
                            verifier: PinVerifier,
                        ): Boolean {
                            submittedPins += pin.concatToString()
                            return true
                        }

                        override suspend fun setBiometricsEnabled(enabled: Boolean) = Unit
                    },
                ),
            )
        composeRule.setContent {
            CashEyeTheme(darkTheme = false, dynamicColor = false) {
                AppLockScreen(
                    verifier = PinVerifier("salt", "hash"),
                    biometricsEnabled = false,
                    onRequestBiometricAuthentication = {},
                    onPinVerificationError = {},
                    onPinVerify = {},
                    viewModel = viewModel,
                )
            }
        }

        listOf(1, 2, 3, 4).forEach { digit ->
            composeRule.onNodeWithTag("app_lock_key_$digit").performClick()
        }
        composeRule.mainClock.advanceTimeBy(500)

        assertEquals(listOf("1234"), submittedPins)
    }
}
