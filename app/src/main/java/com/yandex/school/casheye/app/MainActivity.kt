package com.yandex.school.casheye.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yandex.school.casheye.app.navigation.NavigationRoot
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.splash.presentation.SplashScreen
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val appGraph = (application as CashEyeApplication).appGraph
        setContent {
            CompositionLocalProvider(
                LocalMetroViewModelFactory provides appGraph.metroViewModelFactory,
            ) {
                CashEyeTheme {
                    var isSplashVisible by rememberSaveable { mutableStateOf(!SplashSession.hasFinishedLottie) }

                    if (isSplashVisible) {
                        SplashScreen(
                            onFinish = {
                                SplashSession.hasFinishedLottie = true
                                isSplashVisible = false
                            },
                        )
                    } else {
                        NavigationRoot(networkStatus = appGraph.networkMonitor.isOnline)
                    }
                }
            }
        }
    }
}

private object SplashSession {
    var hasFinishedLottie: Boolean = false
}
