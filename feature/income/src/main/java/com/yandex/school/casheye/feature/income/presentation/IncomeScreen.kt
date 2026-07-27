package com.yandex.school.casheye.feature.income.presentation

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.income.R

@Composable
fun IncomeScreen(
    state: IncomeUiState,
    onIntent: (IncomeIntent) -> Unit,
    modifier: Modifier = Modifier,
    onTransactionClick: (Int) -> Unit = {},
) {
    PullToRefreshContainer(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(IncomeIntent.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            when (state) {
                IncomeUiState.Loading -> {
                    DelayedCircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is IncomeUiState.Empty -> {
                    EmptyIncome(modifier = Modifier.align(Alignment.Center))
                }

                is IncomeUiState.Content -> {
                    IncomeContent(
                        state = state,
                        onTransactionClick = onTransactionClick,
                        onTransactionDelete = { onIntent(IncomeIntent.DeleteTransaction(it)) },
                    )
                }

                is IncomeUiState.Error -> {
                    ErrorState(
                        type = state.reason.toErrorStateType(),
                        onRetry = { onIntent(IncomeIntent.Retry) },
                        retryLabel = stringResource(R.string.retry),
                    )
                }
            }
        }
    }
}

@Composable
private fun IncomeContent(
    state: IncomeUiState.Content,
    onTransactionClick: (Int) -> Unit,
    onTransactionDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        NativeMoneySummary(
            title = stringResource(R.string.income_total),
            nativeTotals =
                state.nativeTotals.map {
                    formatAmount(
                        amount = it.amount,
                        currencyCode = it.currency.isoCode,
                    )
                },
        )
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
            contentPadding = PaddingValues(bottom = 60.dp),
        ) {
            items(
                items = state.transactions,
                key = Transaction::id,
            ) { transaction ->
                SwipeToRevealDeleteItem(
                    actionLabel = stringResource(R.string.delete_income),
                    onClick = { onTransactionClick(transaction.id) },
                    onDelete = { onTransactionDelete(transaction.id) },
                ) {
                    MoneyListItem(
                        emoji = transaction.category.emoji,
                        title = transaction.category.name,
                        amount =
                            formatAmount(
                                amount = transaction.amount,
                                currencyCode = transaction.currency.isoCode,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyIncome(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.image_empty_income),
            contentDescription = stringResource(R.string.empty_income),
            modifier =
                Modifier
                    .size(200.dp),
        )
        Text(
            text = stringResource(R.string.empty_income),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_income_description),
            modifier = Modifier.widthIn(max = 280.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
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
private fun IncomeScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            IncomeScreen(
                state = incomeUiStateMock,
                onIntent = {},
            )
        }
    }
}
