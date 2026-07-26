package com.yandex.school.casheye.feature.accounts.presentation

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.DelayedCircularProgressIndicator
import com.yandex.school.casheye.core.designsystem.component.ErrorState
import com.yandex.school.casheye.core.designsystem.component.ErrorStateType
import com.yandex.school.casheye.core.designsystem.component.MoneyListItem
import com.yandex.school.casheye.core.designsystem.component.NativeMoneySummary
import com.yandex.school.casheye.core.designsystem.component.PullToRefreshContainer
import com.yandex.school.casheye.core.designsystem.component.SwipeToRevealDeleteItem
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.accounts.R
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onIntent: (AccountsIntent) -> Unit,
    onAccountClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    PullToRefreshContainer(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(AccountsIntent.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
        ) {
            when (state) {
                AccountsUiState.Loading -> {
                    DelayedCircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is AccountsUiState.Empty -> {
                    EmptyAccounts(modifier = Modifier.align(Alignment.Center))
                }

                is AccountsUiState.Content -> {
                    AccountsContent(
                        state = state,
                        onAccountClick = onAccountClick,
                        onAccountDelete = { onIntent(AccountsIntent.RequestAccountDelete(it)) },
                    )
                }

                is AccountsUiState.Error -> {
                    ErrorState(
                        type = state.reason.toErrorStateType(),
                        onRetry = { onIntent(AccountsIntent.Retry) },
                        retryLabel = stringResource(R.string.retry),
                    )
                }
            }
        }
    }

    val confirmation = (state as? AccountsUiState.Content)?.deleteConfirmation
    if (confirmation != null) {
        AlertDialog(
            onDismissRequest = { onIntent(AccountsIntent.CancelAccountDelete) },
            title = { Text(stringResource(R.string.delete_account_confirmation_title)) },
            text = {
                Text(
                    pluralStringResource(
                        R.plurals.delete_account_confirmation,
                        confirmation.transactionCount,
                        confirmation.transactionCount,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { onIntent(AccountsIntent.ConfirmAccountDelete) }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(AccountsIntent.CancelAccountDelete) }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun AccountsContent(
    state: AccountsUiState.Content,
    onAccountClick: (Int) -> Unit,
    onAccountDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AccountsHero(
            state = state,
        )
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(bottom = 60.dp),
        ) {
            items(
                items = state.accounts,
                key = Account::id,
            ) { accountItem ->
                SwipeToRevealDeleteItem(
                    actionLabel = stringResource(R.string.delete_account),
                    onClick = { onAccountClick(accountItem.id) },
                    onDelete = { onAccountDelete(accountItem.id) },
                ) {
                    MoneyListItem(
                        emoji = accountItem.emoji,
                        title = accountItem.name,
                        amount =
                            formatAmount(
                                amount = accountItem.balance,
                                currencyCode = accountItem.currency.isoCode,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAccounts(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.image_empty_accounts),
            contentDescription = stringResource(R.string.empty_accounts),
            modifier =
                Modifier
                    .size(200.dp),
        )
        Text(
            text = stringResource(R.string.empty_accounts),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_accounts_description),
            modifier = Modifier.widthIn(max = 280.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AccountsHero(state: AccountsUiState.Content) {
    val valuation = state.currentValuation
    val included =
        valuation
            ?.includedTotal
            ?.let { formatAmount(it.amount, it.currency.isoCode) }
    val date =
        valuation
            ?.rateDate
            ?.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val valuationText =
        included?.let {
            when {
                valuation?.isComplete == true && date != null -> {
                    stringResource(R.string.balance_valuation_dated, it, date)
                }

                valuation?.isComplete == true -> {
                    stringResource(R.string.balance_valuation, it)
                }

                date != null -> {
                    stringResource(R.string.balance_valuation_partial_dated, it, date)
                }

                else -> {
                    stringResource(R.string.balance_valuation_partial, it)
                }
            }
        }
    val excluded =
        valuation
            ?.excludedNativeTotals
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString(separator = " · ") {
                formatAmount(it.amount, it.currency.isoCode)
            }?.let { stringResource(R.string.balance_not_included, it) }

    NativeMoneySummary(
        title = stringResource(R.string.balance_total),
        nativeTotals =
            state.nativeTotals.map {
                formatAmount(it.amount, it.currency.isoCode)
            },
        valuation = valuationText,
        warning = excluded,
    )
}

private fun FinanceFailureReason.toErrorStateType(): ErrorStateType =
    when (this) {
        FinanceFailureReason.Network -> ErrorStateType.Network
        FinanceFailureReason.Authorization -> ErrorStateType.Authorization
        FinanceFailureReason.Server -> ErrorStateType.Server
        FinanceFailureReason.Unknown -> ErrorStateType.Unknown
    }

@Preview(
    name = "Light",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 412,
    heightDp = 892,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun AccountsScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            AccountsScreen(state = accountsUiStateMock, onIntent = {})
        }
    }
}
