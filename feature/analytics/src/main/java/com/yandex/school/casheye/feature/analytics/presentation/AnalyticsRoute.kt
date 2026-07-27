package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.core.designsystem.component.snackbar.DismissSnackbarOnDispose
import com.yandex.school.casheye.core.designsystem.component.snackbar.showRetrySnackbar
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.analytics.R
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AnalyticsRoute(
    entryPoint: AnalyticsEntryPoint,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    networkRecoveryRefreshId: Long? = null,
    onNetworkRecoveryRefresh: (Long) -> Unit = {},
    viewModel: AnalyticsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val failureMessages = localizedFailureMessages()
    val retryLabel = stringResource(R.string.retry)
    val currentOnNetworkRecoveryRefresh by rememberUpdatedState(onNetworkRecoveryRefresh)

    DismissSnackbarOnDispose(snackbarHostState)

    LaunchedEffect(entryPoint, viewModel) {
        viewModel.onIntent(AnalyticsIntent.Initialize(entryPoint))
    }

    LaunchedEffect(networkRecoveryRefreshId, viewModel) {
        val refreshId = networkRecoveryRefreshId ?: return@LaunchedEffect
        viewModel.onIntent(AnalyticsIntent.NetworkRecovered)
        currentOnNetworkRecoveryRefresh(refreshId)
    }
    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is AnalyticsEffect.ShowError -> {
                    if (
                        snackbarHostState.showRetrySnackbar(
                            message = failureMessages.getValue(effect.reason),
                            retryLabel = retryLabel,
                        )
                    ) {
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
