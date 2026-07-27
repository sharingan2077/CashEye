package com.yandex.school.casheye.feature.expenses.presentation

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
import com.yandex.school.casheye.core.designsystem.component.snackbar.showSuccessSnackbar
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.expenses.R
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun ExpensesRoute(
    selectedDate: LocalDate,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onTransactionClick: (Int) -> Unit = {},
    networkRecoveryRefreshId: Long? = null,
    onNetworkRecoveryRefresh: (Long) -> Unit = {},
    viewModel: ExpensesViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val failureMessages = localizedFailureMessages()
    val retryLabel = stringResource(R.string.retry)
    val deletedMessage = stringResource(R.string.expense_deleted)
    val deleteErrorMessage = stringResource(R.string.error_delete_expense)
    val currentOnNetworkRecoveryRefresh by rememberUpdatedState(onNetworkRecoveryRefresh)

    DismissSnackbarOnDispose(snackbarHostState)

    LaunchedEffect(selectedDate, viewModel) {
        viewModel.onIntent(ExpensesIntent.SelectDate(selectedDate))
    }

    LaunchedEffect(networkRecoveryRefreshId, viewModel) {
        val refreshId = networkRecoveryRefreshId ?: return@LaunchedEffect
        viewModel.onIntent(ExpensesIntent.NetworkRecovered)
        currentOnNetworkRecoveryRefresh(refreshId)
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ExpensesEffect.ShowError -> {
                    if (
                        snackbarHostState.showRetrySnackbar(
                            message = failureMessages.getValue(effect.reason),
                            retryLabel = retryLabel,
                        )
                    ) {
                        viewModel.onIntent(ExpensesIntent.Retry)
                    }
                }

                is ExpensesEffect.ShowDeleteError -> {
                    snackbarHostState.showSnackbar(
                        if (effect.reason == FinanceFailureReason.Unknown) {
                            deleteErrorMessage
                        } else {
                            failureMessages.getValue(effect.reason)
                        },
                    )
                }

                ExpensesEffect.TransactionDeleted -> {
                    snackbarHostState.showSuccessSnackbar(deletedMessage)
                }
            }
        }
    }

    ExpenseScreen(
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
        FinanceFailureReason.Unknown to stringResource(R.string.error_load_expenses),
    )
