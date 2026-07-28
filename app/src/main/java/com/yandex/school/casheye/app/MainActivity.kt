package com.yandex.school.casheye.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var isLottieReady by mutableStateOf(SplashPlaybackState.hasFinished)
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isLottieReady }
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        val appGraph = (application as CashEyeApplication).appGraph
        setContent {
            CashEyeApp(
                metroViewModelFactory = appGraph.metroViewModelFactory,
                networkStatus = appGraph.networkMonitor.isOnline,
                observeSettings = appGraph.observeSettings,
                onSplashReady = { isLottieReady = true },
            )
        }
    }
}
