package com.yandex.school.casheye.feature.income.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    viewModel: IncomeViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val failureMessages = localizedFailureMessages()
    val retryLabel = stringResource(R.string.retry)

    LaunchedEffect(selectedDate, viewModel) {
        viewModel.onIntent(IncomeIntent.SelectDate(selectedDate))
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is IncomeEffect.ShowError -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = failureMessages.getValue(effect.reason),
                            actionLabel = retryLabel,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(IncomeIntent.Retry)
                    }
                }
            }
        }
    }
    IncomeScreen(
        state = state,
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
