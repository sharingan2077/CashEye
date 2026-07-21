@file:Suppress("TooManyFunctions")

package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.yandex.school.casheye.core.designsystem.component.EmojiCircle
import com.yandex.school.casheye.core.designsystem.component.ErrorState
import com.yandex.school.casheye.core.designsystem.component.ErrorStateType
import com.yandex.school.casheye.core.designsystem.component.FilterItem
import com.yandex.school.casheye.core.designsystem.component.IconCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.feature.analytics.R

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onIntent: (AnalyticsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state) {
            is AnalyticsUiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            is AnalyticsUiState.Content -> AnalyticsContent(state, onIntent)
            is AnalyticsUiState.Empty -> AnalyticsEmpty(state, onIntent)
            is AnalyticsUiState.Error -> ErrorState(type = state.reason.toErrorStateType())
        }
    }
    AnalyticsBottomSheet(state = state, onIntent = onIntent)
}

@Composable
private fun AnalyticsContent(
    state: AnalyticsUiState.Content,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    val chartModelProducer = remember { PieChartModelProducer() }
    var animateChartIn by remember { mutableStateOf(true) }
    LaunchedEffect(state.categorySummaries) {
        chartModelProducer.runTransaction {
            pieSeries { series(analyticsPieChartValues(state.categorySummaries)) }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AnalyticsPieChart(
                total = formatAmount(state.total, state.currencyCode),
                categories = state.categorySummaries,
                modelProducer = chartModelProducer,
                animateIn = animateChartIn,
                onChartDispose = { animateChartIn = false },
                modifier =
                    Modifier.clickable(
                        role = Role.Button,
                        onClick = { onIntent(AnalyticsIntent.OpenDetails) },
                    ),
            )
        }
        item { FilterView(data = state.data, onIntent = onIntent) }
        item { TransactionsHeading() }
        itemsIndexed(
            items = state.transactions,
            key = { _, transaction -> transaction.id },
        ) { index: Int, transaction: Transaction ->
            TransactionItem(
                emoji = transaction.category.emoji,
                title = transaction.category.name,
                comment = transaction.comment,
                amount = formatAmount(transaction.amount, state.currencyCode),
            )
            if (index != state.transactions.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun AnalyticsEmpty(
    state: AnalyticsUiState.Empty,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterView(data = state.data, onIntent = onIntent)
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AnalyticsEmptyState()
        }
    }
}

@Composable
private fun AnalyticsEmptyState() {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.image_empty_analytics),
            contentDescription = stringResource(R.string.empty_period),
            modifier =
                Modifier
                    .size(200.dp),
        )
        Text(
            text = stringResource(R.string.empty_period),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.empty_period_description),
            modifier = Modifier.widthIn(max = 280.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TransactionsHeading() {
    Text(
        text = stringResource(R.string.transactions),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
    )
}

@Composable
internal fun TransactionItem(
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
                comment?.takeIf(String::isNotBlank)?.let { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        trail = {
            Text(text = amount, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
        },
    )
}

@Composable
private fun FilterView(
    data: AnalyticsScreenData,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    val filters = data.filters
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        AnalyticsFilterItem(
            iconPainter = painterResource(R.drawable.list),
            title = stringResource(R.string.filter_type),
            value = filters.type.title(),
            onClick = { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Type)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        AnalyticsFilterItem(
            iconPainter = painterResource(R.drawable.calendar),
            title = stringResource(R.string.filter_period),
            value = filters.period.formatted(),
            onClick = { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Period)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        AnalyticsFilterItem(
            iconPainter = painterResource(R.drawable.tag),
            title = stringResource(R.string.filter_categories),
            value = categoriesTitle(filters.categoryIds, data.categories),
            onClick = { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Categories)) },
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        AnalyticsFilterItem(
            iconPainter = painterResource(R.drawable.credit_card),
            title = stringResource(R.string.filter_account),
            value =
                data.accounts.firstOrNull { it.id == filters.accountId }?.name
                    ?: stringResource(R.string.all_accounts),
            onClick = { onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Account)) },
        )
    }
}

@Composable
private fun AnalyticsFilterItem(
    iconPainter: Painter,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        lead = { IconCircle(iconPainter = iconPainter, contentDescription = title) },
        content = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        trail = { FilterItem(title = value) },
        height = 56.dp,
    )
}

@Composable
private fun categoriesTitle(
    selectedIds: Set<Int>,
    categories: List<Category>,
): String {
    if (selectedIds.isEmpty()) return stringResource(R.string.all_categories)
    val names = categories.filter { it.id in selectedIds }.map { it.name }
    return when {
        names.isEmpty() -> stringResource(R.string.selected_count, selectedIds.size)
        names.size <= 2 -> names.joinToString()
        else -> stringResource(R.string.selected_count, selectedIds.size)
    }
}

@Composable
private fun AnalyticsType.title(): String =
    stringResource(
        when (this) {
            AnalyticsType.Expenses -> R.string.type_expenses
            AnalyticsType.Income -> R.string.type_income
            AnalyticsType.All -> R.string.type_all
        },
    )

private fun FinanceFailureReason.toErrorStateType(): ErrorStateType =
    when (this) {
        FinanceFailureReason.Network -> ErrorStateType.Network
        FinanceFailureReason.Authorization -> ErrorStateType.Authorization
        FinanceFailureReason.Server -> ErrorStateType.Server
        FinanceFailureReason.Unknown -> ErrorStateType.Unknown
    }
