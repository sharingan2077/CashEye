package com.yandex.school.casheye.feature.expenses.presentation

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.yandex.school.casheye.core.designsystem.component.PullToRefreshContainer
import com.yandex.school.casheye.core.designsystem.component.ScrollToTopButton
import com.yandex.school.casheye.core.designsystem.component.SwipeToRevealDeleteItem
import com.yandex.school.casheye.core.designsystem.component.money.MoneyListItem
import com.yandex.school.casheye.core.designsystem.component.money.NativeMoneySummary
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.expenses.R
import com.yandex.school.casheye.feature.expenses.presentation.preview.expensesUiStateMock

@Composable
fun ExpenseScreen(
    state: ExpensesUiState,
    onIntent: (ExpensesIntent) -> Unit,
    modifier: Modifier = Modifier,
    onTransactionClick: (Int) -> Unit = {},
) {
    PullToRefreshContainer(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(ExpensesIntent.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            when (state) {
                ExpensesUiState.Loading -> {
                    DelayedCircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is ExpensesUiState.Empty -> {
                    EmptyExpenses(modifier = Modifier.align(Alignment.Center))
                }

                is ExpensesUiState.Content -> {
                    ExpensesContent(
                        state = state,
                        onTransactionClick = onTransactionClick,
                        onTransactionDelete = { onIntent(ExpensesIntent.DeleteTransaction(it)) },
                    )
                }

                is ExpensesUiState.Error -> {
                    ErrorState(
                        type = state.reason.toErrorStateType(),
                        onRetry = { onIntent(ExpensesIntent.Retry) },
                        retryLabel = stringResource(R.string.retry),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpensesContent(
    state: ExpensesUiState.Content,
    onTransactionClick: (Int) -> Unit,
    onTransactionDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var revealedTransactionId by remember { mutableStateOf<Int?>(null) }
    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 72.dp),
        ) {
            item {
                NativeMoneySummary(
                    title = stringResource(R.string.expenses_total),
                    total =
                        state.currentValuation
                            ?.includedTotal
                            ?.let { formatAmount(it.amount, it.currency.isoCode) },
                    nativeTotals =
                        state.nativeTotals.map {
                            formatAmount(
                                amount = it.amount,
                                currencyCode = it.currency.isoCode,
                            )
                        },
                    warning =
                        state.currentValuation
                            ?.excludedNativeTotals
                            ?.takeIf { it.isNotEmpty() }
                            ?.joinToString(separator = " · ") {
                                formatAmount(it.amount, it.currency.isoCode)
                            }?.let { stringResource(R.string.expenses_not_included, it) },
                )
            }
            items(
                items = state.transactions,
                key = Transaction::id,
            ) { transaction ->
                SwipeToRevealDeleteItem(
                    actionLabel = stringResource(R.string.delete_expense),
                    isRevealed = revealedTransactionId == transaction.id,
                    onReveal = { revealedTransactionId = transaction.id },
                    onDismissReveal = {
                        if (revealedTransactionId == transaction.id) {
                            revealedTransactionId = null
                        }
                    },
                    onClick = {
                        revealedTransactionId = null
                        onTransactionClick(transaction.id)
                    },
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
        ScrollToTopButton(
            listState = listState,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun EmptyExpenses(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.image_empty_expenses),
            contentDescription = null,
            modifier =
                Modifier
                    .size(150.dp),
        )
        Text(
            text = stringResource(R.string.empty_expenses),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_expenses_description),
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
private fun ExpenseScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.surface) {
            ExpenseScreen(
                state = expensesUiStateMock,
                onIntent = {},
            )
        }
    }
}
