package com.yandex.school.casheye.feature.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.ui.theme.CashEyeTheme

@Immutable
private data class ExpenseItem(
    val emoji: String,
    val title: String,
    val amount: String,
)

private val Expenses = listOf(
    ExpenseItem(emoji = "✏\uFE0F", title = "Покупка канцтоваров", amount = "1 200 ₽"),
    ExpenseItem(emoji = "☕", title = "Обед в кафе", amount = "750 ₽"),
    ExpenseItem(emoji = "⛽", title = "Топливо для машины", amount = "2 300 ₽"),
    ExpenseItem(emoji = "\uD83D\uDCF1", title = "Подписка на сервис", amount = "450 ₽"),
    ExpenseItem(emoji = "\uD83D\uDD27", title = "Ремонт техники", amount = "5 800 ₽"),
    ExpenseItem(emoji = "\uD83C\uDFAB", title = "Покупка билетов", amount = "3 200 ₽"),
    ExpenseItem(emoji = "\uD83C\uDF10", title = "Оплата интернета", amount = "800 ₽"),
    ExpenseItem(emoji = "\uD83D\uDED2", title = "Магазин продуктов", amount = "2 450 ₽"),
)

@Composable
fun ExpenseScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ExpensesHero()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(Expenses) { expense ->
                ExpenseRow(expense = expense)
            }
        }
    }
}

@Composable
private fun ExpensesHero() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(117.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 20.dp, top = 12.dp),
    ) {
        Text(
            text = "расходы, всего",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "323 524 ₽",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.displayMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ExpenseRow(expense: ExpenseItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = expense.emoji,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = expense.title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 16.dp),
        )
        Text(
            text = expense.amount,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
        )
    }
}


@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun ExpenseScreenPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface(color = MaterialTheme.colorScheme.background) {
            ExpenseScreen()
        }
    }
}
