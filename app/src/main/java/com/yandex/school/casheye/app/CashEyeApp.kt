package com.yandex.school.casheye.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.app.navigation.NavigationRoot
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.domain.settings.AppSettings
import com.yandex.school.casheye.domain.settings.ObserveSettingsUseCase
import com.yandex.school.casheye.domain.settings.ThemeMode
import com.yandex.school.casheye.feature.splash.presentation.SplashScreen
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun CashEyeApp(
    metroViewModelFactory: MetroViewModelFactory,
    networkStatus: StateFlow<Boolean>,
    observeSettings: ObserveSettingsUseCase,
    biometricsAvailable: Boolean,
    requestBiometricAuthentication: (onResult: (Boolean) -> Unit) -> Unit,
    onSplashReady: () -> Unit,
) {
    val settings by observeSettings().collectAsStateWithLifecycle(initialValue = null as AppSettings?)
    var savedThemeModeName by rememberSaveable { mutableStateOf<String?>(null) }
    val themeMode =
        settings?.themeMode
            ?: savedThemeModeName?.let(ThemeMode::valueOf)
            ?: ThemeMode.SYSTEM
    val darkTheme =
        when (themeMode) {
            ThemeMode.LIGHT -> false

            ThemeMode.DARK -> true

            ThemeMode.SYSTEM -> isSystemInDarkTheme()
        }

    LaunchedEffect(settings?.themeMode) {
        settings?.themeMode?.let { mode ->
            savedThemeModeName = mode.name
            AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
        }
    }

    LaunchedEffect(settings?.language) {
        settings?.language?.let { language ->
            val locales = LocaleListCompat.forLanguageTags(language.languageTag.orEmpty())
            if (AppCompatDelegate.getApplicationLocales() != locales) {
                AppCompatDelegate.setApplicationLocales(locales)
            }
        }
    }

    CompositionLocalProvider(
        LocalMetroViewModelFactory provides metroViewModelFactory,
    ) {
        CashEyeTheme(darkTheme = darkTheme, dynamicColor = false) {
            if (SplashPlaybackState.hasFinished) {
                val currentSettings = settings
                if (currentSettings != null) {
                    SystemBarAppearanceEffect(darkTheme = darkTheme)
                    AppLockGate(
                        security = currentSettings.security,
                        biometricsAvailable = biometricsAvailable,
                        requestBiometricAuthentication = requestBiometricAuthentication,
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

@Composable
private fun SystemBarAppearanceEffect(darkTheme: Boolean) {
    val view = LocalView.current

    SideEffect {
        val insetsController =
            WindowCompat.getInsetsController(
                view.context.findActivity().window,
                view,
            )
        insetsController.isAppearanceLightStatusBars = !darkTheme
    }
}

private tailrec fun Context.findActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> error("CashEyeApp must be hosted in an Activity")
    }

private fun ThemeMode.toNightMode(): Int =
    when (this) {
        ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
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
