package com.yandex.school.casheye.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.yandex.school.casheye.app.navigation.NavigationRoot
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.splash.presentation.SplashScreen
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import dev.zacsweers.metrox.viewmodel.MetroViewModelFactory
import kotlinx.coroutines.flow.StateFlow

@Composable
internal fun CashEyeApp(
    metroViewModelFactory: MetroViewModelFactory,
    networkStatus: StateFlow<Boolean>,
    onSplashReady: () -> Unit,
) {
    CompositionLocalProvider(
        LocalMetroViewModelFactory provides metroViewModelFactory,
    ) {
        CashEyeTheme {
            if (SplashPlaybackState.hasFinished) {
                NavigationRoot(networkStatus = networkStatus)
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
