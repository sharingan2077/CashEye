package com.yandex.school.casheye.core.designsystem.component.snackbar

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

suspend fun SnackbarHostState.showRetrySnackbar(
    message: String,
    retryLabel: String,
): Boolean =
    showSnackbar(
        message = message,
        actionLabel = retryLabel,
        duration = SnackbarDuration.Long,
    ) == SnackbarResult.ActionPerformed

@Composable
fun DismissSnackbarOnDispose(snackbarHostState: SnackbarHostState) {
    DisposableEffect(snackbarHostState) {
        onDispose { snackbarHostState.currentSnackbarData?.dismiss() }
    }
}
