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
import com.yandex.school.casheye.domain.settings.ThemeMode
import com.yandex.school.casheye.feature.settings.R
import junit.framework.TestCase.assertEquals
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
        composeRule.setContent {
            CashEyeTheme(darkTheme = false, dynamicColor = false) {
                AppLockScreen(
                    biometricsEnabled = false,
                    onRequestBiometricAuthentication = {},
                    onVerifyPin = {
                        submittedPins += it.concatToString()
                        true
                    },
                    onPinVerified = {},
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
