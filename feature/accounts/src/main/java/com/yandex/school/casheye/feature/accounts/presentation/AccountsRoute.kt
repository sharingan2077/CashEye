package com.yandex.school.casheye.feature.accounts.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.core.designsystem.component.snackbar.DismissSnackbarOnDispose
import com.yandex.school.casheye.core.designsystem.component.snackbar.showRetrySnackbar
import com.yandex.school.casheye.core.designsystem.component.snackbar.showSuccessSnackbar
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.accounts.R
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AccountsRoute(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onAccountClick: (Int) -> Unit = {},
    networkRecoveryRefreshId: Long? = null,
    onNetworkRecoveryRefreshConsumed: (Long) -> Unit = {},
    viewModel: AccountsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val failureMessages = localizedFailureMessages()
    val retryLabel = stringResource(R.string.retry)
    val accountDeletedMessage = stringResource(R.string.account_deleted)
    val deleteErrorMessage = stringResource(R.string.error_delete_account)

    LaunchedEffect(networkRecoveryRefreshId, viewModel) {
        val refreshId = networkRecoveryRefreshId ?: return@LaunchedEffect
        viewModel.onIntent(AccountsIntent.NetworkRecovered)
        onNetworkRecoveryRefreshConsumed(refreshId)
    }
    val resources = LocalResources.current

    DismissSnackbarOnDispose(snackbarHostState)

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is AccountsEffect.ShowError -> {
                    if (
                        snackbarHostState.showRetrySnackbar(
                            message = failureMessages.getValue(effect.reason),
                            retryLabel = retryLabel,
                        )
                    ) {
                        viewModel.onIntent(AccountsIntent.Retry)
                    }
                }

                is AccountsEffect.ShowDeleteError -> {
                    snackbarHostState.showSnackbar(
                        if (effect.reason == FinanceFailureReason.Unknown) {
                            deleteErrorMessage
                        } else {
                            failureMessages.getValue(effect.reason)
                        },
                    )
                }

                is AccountsEffect.AccountDeleted -> {
                    val message =
                        if (effect.transactionCount == 0) {
                            accountDeletedMessage
                        } else {
                            resources.getQuantityString(
                                R.plurals.account_with_transactions_deleted,
                                effect.transactionCount,
                                effect.transactionCount,
                            )
                        }
                    snackbarHostState.showSuccessSnackbar(message)
                }
            }
        }
    }

    AccountsScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onAccountClick = onAccountClick,
        modifier = modifier,
    )
}

@Composable
private fun localizedFailureMessages(): Map<FinanceFailureReason, String> =
    mapOf(
        FinanceFailureReason.Network to stringResource(R.string.error_network),
        FinanceFailureReason.Authorization to stringResource(R.string.error_authorization),
        FinanceFailureReason.Server to stringResource(R.string.error_server),
        FinanceFailureReason.Unknown to stringResource(R.string.error_load_accounts),
    )
