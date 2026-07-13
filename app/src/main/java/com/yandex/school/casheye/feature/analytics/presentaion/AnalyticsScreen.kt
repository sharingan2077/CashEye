package com.yandex.school.casheye.feature.analytics.presentaion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yandex.school.casheye.core.designsystem.component.EmojiCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Transaction

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    state: AnalyticsUiState = analyticsUiStateMock,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(top = 32.dp),
    ) {
        item {
            AnalyticsView(
                total = formatAmount(state.total, state.currencyCode),
                articles = analyticsArticles,
            )
        }
        item { FilterView(filters = state.filters) }
        analyticsTransactions(transactions = state.transactions)
    }
}

private fun LazyListScope.analyticsTransactions(transactions: List<Transaction>) {
    item { TransactionsHeading() }
    items(items = transactions, key = Transaction::id) { transaction ->
        Column {
            TransactionItem(
                emoji = transaction.category.emoji,
                title = transaction.category.name,
                comment = transaction.comment,
                amount = formatAmount(transaction.amount, "RUB"),
            )
            Spacer(modifier = Modifier.background(MaterialTheme.colorScheme.outline))
        }
    }
}

@Composable
private fun TransactionsHeading() {
    Box(
        modifier =
            Modifier
                .padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp)
                .wrapContentHeight()
                .wrapContentWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Транзакции",
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
        )
    }
}

@Composable
fun TransactionItem(
    emoji: String,
    title: String,
    comment: String?,
    amount: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        lead = { EmojiCircle(emoji = emoji) },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                if (comment != null) {
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trail = {
            Text(
                text = amount,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.End,
            )
        },
    )
}

@Composable
fun FilterView(
    filters: List<Filter>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        filters.forEach { filter ->
            AnalyticsFilterItem(
                iconPainter = painterResource(filter.resId),
                contentDescription = filter.title,
                title = filter.title,
                filter = filter.value,
            )
            Spacer(modifier = Modifier.background(MaterialTheme.colorScheme.outline))
        }
    }
}

private val Filter.value: String
    get() =
        when (this) {
            is Filter.Type -> type
            is Filter.Period -> period
            is Filter.Articles -> articles.toString()
            is Filter.Account -> accounts.toString()
        }

@Composable
fun AnalyticsFilterItem(
    iconPainter: Painter,
    contentDescription: String?,
    title: String,
    filter: String,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier,
        lead = { IconCircle(iconPainter = iconPainter, contentDescription = contentDescription) },
        content = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trail = { FilterItem(title = filter) },
        height = 56.dp,
    )
}

@Composable
fun FilterItem(
    title: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(24.dp)
                .wrapContentWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
fun IconCircle(
    iconPainter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(32.dp)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painter = iconPainter, contentDescription = contentDescription)
    }
}

@Composable
private fun AnalyticsView(
    total: String,
    articles: Map<String, Int>,
    modifier: Modifier = Modifier,
) {
    AnalyticsPieChart(total = total, articles = articles, modifier = modifier)
}

private val analyticsArticles =
    mapOf(
        "Ремонт" to 50000,
        "Авто" to 45000,
        "Другое" to 30000,
    )

@Preview(showBackground = true)
@Composable
private fun AnalyticsScreenPreview() {
    CashEyeTheme(dynamicColor = false) { AnalyticsScreen() }
}
