package com.yandex.school.casheye.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.app.navigation.NavigationRoot
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.domain.settings.AppSettings
import com.yandex.school.casheye.domain.settings.ObserveSettingsUseCase
import com.yandex.school.casheye.domain.settings.ThemeMode
import com.yandex.school.casheye.domain.settings.VerifyPinUseCase
import com.yandex.school.casheye.feature.splash.presentation.SplashScreen
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun CashEyeApp(
    metroViewModelFactory: MetroViewModelFactory,
    networkStatus: StateFlow<Boolean>,
    observeSettings: ObserveSettingsUseCase,
    verifyPin: VerifyPinUseCase,
    biometricsAvailable: Boolean,
    requestBiometricAuthentication: (onResult: (Boolean) -> Unit) -> Unit,
    onSplashReady: () -> Unit,
) {
    val settings by observeSettings().collectAsStateWithLifecycle(initialValue = null as AppSettings?)
    val darkTheme =
        when (settings?.themeMode) {
            ThemeMode.LIGHT -> false

            ThemeMode.DARK -> true

            ThemeMode.SYSTEM,
            null,
            -> isSystemInDarkTheme()
        }

    LaunchedEffect(settings?.language) {
        settings?.language?.let { language ->
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.languageTag.orEmpty()),
            )
        }
    }

    CompositionLocalProvider(
        LocalMetroViewModelFactory provides metroViewModelFactory,
    ) {
        CashEyeTheme(darkTheme = darkTheme, dynamicColor = false) {
            if (SplashPlaybackState.hasFinished) {
                val currentSettings = settings
                if (currentSettings != null) {
                    AppLockGate(
                        security = currentSettings.security,
                        biometricsAvailable = biometricsAvailable,
                        requestBiometricAuthentication = requestBiometricAuthentication,
                        verifyPin = { pin ->
                            currentSettings.security.pinVerifier?.let { verifier ->
                                verifyPin(pin, verifier)
                            } == true
                        },
                    ) {
                        NavigationRoot(
                            networkStatus = networkStatus,
                            biometricsAvailable = biometricsAvailable,
                            onRequestBiometricAuthentication = requestBiometricAuthentication,
                        )
                    }
                }
            } else {
                SplashScreen(
                    onReady = onSplashReady,
                    onFinish = {
                        onSplashReady()
                        SplashPlaybackState.markFinished()
                    },
                )
            }
        }
    }
}

/**
 * Process-local state: keeps the Lottie splash from replaying after Activity recreation,
 * but deliberately resets after process death so the system splash can always be released.
 */
internal object SplashPlaybackState {
    var hasFinished by mutableStateOf(false)
        private set

    fun markFinished() {
        hasFinished = true
    }
}
