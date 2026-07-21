package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.analytics.R
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
    val failureMessages = localizedFailureMessages()
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(entryPoint, viewModel) {
        viewModel.onIntent(AnalyticsIntent.Initialize(entryPoint))
    }
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is AnalyticsEffect.ShowError -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = failureMessages.getValue(effect.reason),
                            actionLabel = retryLabel,
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

@Composable
private fun localizedFailureMessages(): Map<FinanceFailureReason, String> =
    mapOf(
        FinanceFailureReason.Network to stringResource(R.string.error_network),
        FinanceFailureReason.Authorization to stringResource(R.string.error_authorization),
        FinanceFailureReason.Server to stringResource(R.string.error_server),
        FinanceFailureReason.Unknown to stringResource(R.string.error_load_analytics),
    )
