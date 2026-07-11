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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.core.view.WindowCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieClipSpec
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.yandex.school.casheye.R

@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val splashBackground = colorResource(R.color.splash_background)

    SplashNavigationBarEffect(
        navigationBarColor = splashBackground.toArgb(),
    )

    val compositionResult = rememberLottieComposition(
        spec = LottieCompositionSpec.RawRes(R.raw.splash_screen_animation),
    )
    val composition by compositionResult
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        clipSpec = LottieClipSpec.Progress(
            min = 0f,
            max = SPLASH_END_PROGRESS,
        ),
    )

    LaunchedEffect(compositionResult.isFailure, composition, progress) {
        if (
            compositionResult.isFailure ||
            composition != null && progress >= SPLASH_END_PROGRESS
        ) {
            onFinished()
        }
    }

    Box(
        modifier = modifier
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

@Suppress("DEPRECATION")
@Composable
private fun SplashNavigationBarEffect(navigationBarColor: Int) {
    val view = LocalView.current

    DisposableEffect(view, navigationBarColor) {
        val window = view.context.findActivity().window
        val insetsController = WindowCompat.getInsetsController(window, view)
        val previousColor = window.navigationBarColor
        val previousLightIcons = insetsController.isAppearanceLightNavigationBars
        val previousContrastEnforced = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced
        } else {
            null
        }

        window.navigationBarColor = navigationBarColor
        insetsController.isAppearanceLightNavigationBars = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        onDispose {
            window.navigationBarColor = previousColor
            insetsController.isAppearanceLightNavigationBars = previousLightIcons
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && previousContrastEnforced != null) {
                window.isNavigationBarContrastEnforced = previousContrastEnforced
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("SplashScreen must be hosted in an Activity")
}

private const val SPLASH_END_PROGRESS = 0.6f
