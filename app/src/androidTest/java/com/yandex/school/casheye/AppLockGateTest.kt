package com.yandex.school.casheye

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.yandex.school.casheye.app.AppLockGate
import com.yandex.school.casheye.app.di.AppMetroViewModelFactory
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.domain.settings.AppLanguage
import com.yandex.school.casheye.domain.settings.AppSettings
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.domain.settings.SecuritySettings
import com.yandex.school.casheye.domain.settings.SettingsRepository
import com.yandex.school.casheye.domain.settings.ThemeMode
import com.yandex.school.casheye.domain.settings.VerifyPinUseCase
import com.yandex.school.casheye.feature.settings.presentation.AppLockViewModel
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test

class AppLockGateTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun incorrectPinKeepsContentLocked() {
        setLockGate()

        repeat(4) { composeRule.onNodeWithTag("app_lock_key_9").performClick() }

        composeRule.onNodeWithTag("locked_content").assertDoesNotExist()
    }

    @Test
    fun correctPinUnlocksContent() {
        setLockGate()

        listOf(1, 2, 3, 4).forEach { digit ->
            composeRule.onNodeWithTag("app_lock_key_$digit").performClick()
        }
        composeRule.mainClock.advanceTimeBy(SUCCESSFUL_PIN_UNLOCK_DURATION_MILLIS)

        composeRule.onNodeWithTag("locked_content").assertExists()
    }

    private fun setLockGate() {
        val metroViewModelFactory =
            AppMetroViewModelFactory(
                viewModelProviders =
                    mapOf(
                        AppLockViewModel::class to {
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
                                        ): Boolean = pin.concatToString() == "1234"

                                        override suspend fun setBiometricsEnabled(enabled: Boolean) = Unit
                                    },
                                ),
                            )
                        },
                    ),
                assistedFactoryProviders = emptyMap(),
                manualAssistedFactoryProviders = emptyMap(),
            )
        composeRule.setContent {
            CashEyeTheme(darkTheme = false, dynamicColor = false) {
                CompositionLocalProvider(LocalMetroViewModelFactory provides metroViewModelFactory) {
                    AppLockGate(
                        security = SecuritySettings(pinVerifier = PinVerifier("salt", "hash")),
                        biometricsAvailable = false,
                        requestBiometricAuthentication = {},
                    ) {
                        Box(Modifier.testTag("locked_content"))
                    }
                }
            }
        }
    }

    private companion object {
        const val SUCCESSFUL_PIN_UNLOCK_DURATION_MILLIS = 750L
    }
}
