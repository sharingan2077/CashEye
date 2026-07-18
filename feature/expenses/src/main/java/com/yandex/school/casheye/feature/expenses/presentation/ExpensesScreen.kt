package com.yandex.school.casheye.feature.expenses.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.component.MoneyListItem
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Transaction

@Composable
fun ExpenseScreen(
    state: ExpensesUiState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        when (state) {
            ExpensesUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            ExpensesUiState.Empty -> {
                EmptyExpenses(modifier = Modifier.align(Alignment.Center))
            }

            is ExpensesUiState.Content -> {
                ExpensesContent(state = state)
            }

            is ExpensesUiState.Error -> {
                ExpensesError(
                    message = state.message,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun ExpensesContent(
    state: ExpensesUiState.Content,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        ExpensesHero(
            total =
                formatAmount(
                    amount = state.total,
                    currencyCode = state.currencyCode,
                ),
        )
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
        ) {
            items(
                items = state.transactions,
                key = Transaction::id,
            ) { transaction ->
                MoneyListItem(
                    emoji = transaction.category.emoji,
                    title = transaction.category.name,
                    amount =
                        formatAmount(
                            amount = transaction.amount,
                            currencyCode = transaction.account.currency,
                        ),
                )
            }
        }
    }
}

@Composable
private fun EmptyExpenses(modifier: Modifier = Modifier) {
    Text(
        text = "Расходов нет",
        modifier = modifier.padding(24.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ExpensesError(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ExpensesHero(total: String) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(117.dp)
                .padding(start = 20.dp, top = 12.dp),
    ) {
        Text(
            text = "расходы, всего",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge.copy(lineHeight = 16.sp),
        )
        Text(
            text = total,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExpenseScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpenseScreen(
                state = expensesUiStateMock,
            )
        }
    }
}
