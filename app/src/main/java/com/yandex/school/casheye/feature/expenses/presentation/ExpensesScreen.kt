package com.yandex.school.casheye.feature.expenses.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.core.designsystem.component.MoneyListItem
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Transaction

@Composable
fun ExpenseScreen(
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ExpensesContent(
        state = state,
        modifier = modifier
    )
}

@Composable
private fun ExpensesHero(total: String) {
    Column(
        modifier = Modifier
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

@Composable
private fun ExpensesContent(
    state: ExpensesUiState,
    modifier: Modifier = Modifier
) {


    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ExpensesHero(
            total = formatAmount(
                amount = state.total,
                currencyCode = state.currencyCode,
            ),
        )
        LazyColumn(
            modifier = Modifier
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
                    amount = formatAmount(
                        amount = transaction.amount,
                        currencyCode = transaction.account.currency,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExpenseScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {

            ExpensesContent(state = ExpensesUiState())
        }
    }
}
