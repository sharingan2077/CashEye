package com.yandex.school.casheye.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yandex.school.casheye.app.navigation.NavigationRoot
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.splash.presentation.SplashScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val showLottieSplash = !SplashSession.hasShownLottie
        SplashSession.hasShownLottie = true

        enableEdgeToEdge()
        setContent {
            CashEyeTheme {
                var isSplashVisible by remember { mutableStateOf(showLottieSplash) }

                if (isSplashVisible) {
                    SplashScreen(onFinish = { isSplashVisible = false })
                } else {
                    NavigationRoot()
                }
            }
        }
    }
}

private object SplashSession {
    var hasShownLottie: Boolean = false
}
