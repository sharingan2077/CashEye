package com.yandex.school.casheye

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
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

        repeat(4) { composeRule.onNodeWithTag("app_lock_key_9").performClick() }

        composeRule.onNodeWithTag("locked_content").assertDoesNotExist()
    }

    @Test
    fun correctPinUnlocksContent() {
        setLockGate { pin -> pin.concatToString() == "1234" }

        listOf(1, 2, 3, 4).forEach { digit ->
            composeRule.onNodeWithTag("app_lock_key_$digit").performClick()
        }
        composeRule.mainClock.advanceTimeBy(500)

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
