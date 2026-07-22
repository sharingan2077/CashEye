package com.yandex.school.casheye.feature.income.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.core.designsystem.component.DismissSnackbarOnDispose
import com.yandex.school.casheye.core.designsystem.component.showRetrySnackbar
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.income.R
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun IncomeRoute(
    selectedDate: LocalDate,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    refreshKey: Long = 0,
    onTransactionClick: (Int) -> Unit = {},
    viewModel: IncomeViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val failureMessages = localizedFailureMessages()
    val retryLabel = stringResource(R.string.retry)

    DismissSnackbarOnDispose(snackbarHostState)

    LaunchedEffect(selectedDate, viewModel) {
        viewModel.onIntent(IncomeIntent.SelectDate(selectedDate))
    }

    LaunchedEffect(refreshKey) {
        if (refreshKey > 0) viewModel.onIntent(IncomeIntent.Refresh)
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is IncomeEffect.ShowError -> {
                    if (
                        snackbarHostState.showRetrySnackbar(
                            message = failureMessages.getValue(effect.reason),
                            retryLabel = retryLabel,
                        )
                    ) {
                        viewModel.onIntent(IncomeIntent.Retry)
                    }
                }
            }
        }
    }
    IncomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onTransactionClick = onTransactionClick,
        modifier = modifier,
    )
}

@Composable
private fun localizedFailureMessages(): Map<FinanceFailureReason, String> =
    mapOf(
        FinanceFailureReason.Network to stringResource(R.string.error_network),
        FinanceFailureReason.Authorization to stringResource(R.string.error_authorization),
        FinanceFailureReason.Server to stringResource(R.string.error_server),
        FinanceFailureReason.Unknown to stringResource(R.string.error_load_income),
    )
