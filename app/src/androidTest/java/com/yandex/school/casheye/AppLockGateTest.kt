package com.yandex.school.casheye

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.yandex.school.casheye.app.AppLockGate
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.domain.settings.SecuritySettings
import org.junit.Rule
import org.junit.Test

class AppLockGateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun incorrectPinKeepsContentLocked() {
        setLockGate { false }

        composeRule.onNodeWithTag("app_lock_pin_input").performTextInput("9999")

        composeRule.onNodeWithTag("locked_content").assertDoesNotExist()
    }

    @Test
    fun correctPinUnlocksContent() {
        setLockGate { pin -> pin.concatToString() == "1234" }

        composeRule.onNodeWithTag("app_lock_pin_input").performTextInput("1234")

        composeRule.onNodeWithTag("locked_content").assertExists()
    }

    private fun setLockGate(verifyPin: suspend (CharArray) -> Boolean) {
        composeRule.setContent {
            AppLockGate(
                security = SecuritySettings(pinVerifier = PinVerifier("salt", "hash")),
                biometricsAvailable = false,
                requestBiometricAuthentication = {},
                verifyPin = verifyPin,
            ) {
                Box(Modifier.testTag("locked_content"))
            }
        }
    }
}
