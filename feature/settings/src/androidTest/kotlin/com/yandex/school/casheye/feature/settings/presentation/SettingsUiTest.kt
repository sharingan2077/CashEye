package com.yandex.school.casheye.feature.settings.presentation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.domain.settings.ThemeMode
import com.yandex.school.casheye.feature.settings.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun selectingDarkThemeEmitsDarkMode() {
        var selectedMode by mutableStateOf(ThemeMode.LIGHT)
        composeRule.setContent {
            CashEyeTheme(darkTheme = false, dynamicColor = false) {
                ThemeOption(
                    label = "Dark",
                    painter = painterResource(R.drawable.ic_settings_moon),
                    mode = ThemeMode.DARK,
                    selected = selectedMode == ThemeMode.DARK,
                    onClick = { selectedMode = ThemeMode.DARK },
                )
            }
        }

        composeRule.onNodeWithTag("settings_theme_dark").performClick()

        assertEquals(ThemeMode.DARK, selectedMode)
    }

    @Test
    fun fourthPinDigitSubmitsOnlyDigits() {
        var submittedPin = ""
        composeRule.setContent {
            CashEyeTheme(darkTheme = false, dynamicColor = false) {
                AppLockScreen(
                    biometricsEnabled = false,
                    onPinSubmit = { submittedPin = it.concatToString() },
                )
            }
        }

        composeRule.onNodeWithTag("app_lock_pin_input").performTextInput("12a34")

        assertEquals("1234", submittedPin)
    }
}
