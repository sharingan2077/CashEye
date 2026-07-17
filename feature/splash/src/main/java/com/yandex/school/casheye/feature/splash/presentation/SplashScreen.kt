package com.yandex.school.casheye.feature.splash.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yandex.school.casheye.feature.splash.R

@Composable
fun SplashScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val splashBackground = colorResource(R.color.splash_background)
    val currentOnFinish by rememberUpdatedState(onFinish)

    SplashSystemBarStyleEffect()

    val compositionResult =
        rememberLottieComposition(
            spec = LottieCompositionSpec.RawRes(R.raw.splash_screen_animation),
        )
    val composition by compositionResult
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        clipSpec =
            LottieClipSpec.Progress(
                min = 0f,
                max = SPLASH_END_PROGRESS,
            ),
    )

    LaunchedEffect(compositionResult.isFailure, composition, progress) {
        if (
            compositionResult.isFailure ||
            (composition != null && progress >= SPLASH_END_PROGRESS)
        ) {
            currentOnFinish()
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(splashBackground),
        contentAlignment = Alignment.Center,
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun SplashSystemBarStyleEffect() {
    val view = LocalView.current

    DisposableEffect(view) {
        val window = view.context.findActivity().window
        val insetsController = WindowCompat.getInsetsController(window, view)
        val previousLightIcons = insetsController.isAppearanceLightNavigationBars
        val previousContrastEnforced =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced
            } else {
                null
            }

        insetsController.isAppearanceLightNavigationBars = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        onDispose {
            insetsController.isAppearanceLightNavigationBars = previousLightIcons
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && previousContrastEnforced != null) {
                window.isNavigationBarContrastEnforced = previousContrastEnforced
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> error("SplashScreen must be hosted in an Activity")
    }

private const val SPLASH_END_PROGRESS = 0.6f
