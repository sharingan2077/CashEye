package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AnalyticsRoute(
    entryPoint: AnalyticsEntryPoint,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(entryPoint, viewModel) {
        viewModel.onIntent(AnalyticsIntent.Initialize(entryPoint))
    }
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is AnalyticsEffect.ShowError -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = "Повторить",
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(AnalyticsIntent.Retry)
                    }
                }
            }
        }
    }

    AnalyticsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}
