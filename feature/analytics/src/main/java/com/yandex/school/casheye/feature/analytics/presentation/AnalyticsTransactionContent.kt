package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.yandex.school.casheye.core.designsystem.component.EmojiCircle
import com.yandex.school.casheye.core.designsystem.component.ListItem
import com.yandex.school.casheye.core.designsystem.component.ScrollToTopButton
import com.yandex.school.casheye.domain.finance.AnalyticsTransaction
import com.yandex.school.casheye.domain.finance.UnconvertedAnalyticsTransaction
import com.yandex.school.casheye.feature.analytics.R
import com.yandex.school.casheye.feature.analytics.presentation.chart.AnalyticsPieChart
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsChartPalette
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsOverviewPieChartItems
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsPieChartValues
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsTypePieChartItems
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun AnalyticsContent(
    state: AnalyticsUiState.Content,
    onIntent: (AnalyticsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val chartModelProducer = remember { PieChartModelProducer() }
    var animateChartIn by remember { mutableStateOf(true) }
    val chartPalette = analyticsChartPalette()
    val otherLabel = stringResource(R.string.other)
    val expensesLabel = stringResource(R.string.type_expenses)
    val incomeLabel = stringResource(R.string.type_income)
    val chartItems =
        remember(
            state.data.filters.type,
            state.categorySummaries,
            state.typeSummaries,
            otherLabel,
            expensesLabel,
            incomeLabel,
            chartPalette,
        ) {
            if (state.data.filters.type == AnalyticsType.All) {
                analyticsTypePieChartItems(
                    state.typeSummaries,
                    expensesLabel,
                    incomeLabel,
                    chartPalette,
                )
            } else {
                analyticsOverviewPieChartItems(state.categorySummaries, otherLabel, chartPalette)
            }
        }
    LaunchedEffect(chartItems) {
        chartModelProducer.runTransaction {
            pieSeries {
                series(
                    analyticsPieChartValues(
                        chartItems,
                    ),
                )
            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 72.dp),
        ) {
            if (state.transactions.isNotEmpty()) {
                item {
                    AnalyticsPieChart(
                        total =
                            formatAnalyticsDisplayAmount(
                                state.total,
                                AnalyticsType.All,
                                state.data.filters.type,
                                state.currencyCode,
                            ),
                        items = chartItems,
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
            }
            item { AnalyticsFilterContent(data = state.data, onIntent = onIntent) }
            if (state.unconvertedTransactions.isNotEmpty()) {
                item {
                    MissingRatesNotice(
                        state.unconvertedTransactions,
                        { onIntent(AnalyticsIntent.Retry) },
                    )
                }
            }
            item { TransactionsHeading() }
            itemsIndexed(state.transactions, key = { _, transaction -> transaction.id }) { index, transaction ->
                TransactionItem(
                    emoji = transaction.category.emoji,
                    title = transaction.category.name,
                    comment = transaction.transaction.comment,
                    amount =
                        formatAnalyticsDisplayAmount(
                            transaction.reportingAmount.amount,
                            if (transaction.category.isIncome) AnalyticsType.Income else AnalyticsType.Expenses,
                            state.data.filters.type,
                            state.currencyCode,
                        ),
                    amountSubtitle = transaction.originalAmountSubtitle(state.data.filters.type),
                )
                if (index != state.transactions.lastIndex ||
                    state.unconvertedTransactions.isNotEmpty()
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
            itemsIndexed(
                state.unconvertedTransactions,
                key = { _, transaction -> "missing-${transaction.id}" },
            ) { index, transaction ->
                TransactionItem(
                    emoji = transaction.transaction.category.emoji,
                    title = transaction.transaction.category.name,
                    comment = transaction.transaction.comment,
                    amount =
                        formatAnalyticsDisplayAmount(
                            transaction.originalAmount.amount,
                            if (transaction.transaction.category.isIncome) {
                                AnalyticsType.Income
                            } else {
                                AnalyticsType.Expenses
                            },
                            state.data.filters.type,
                            transaction.originalAmount.currency.isoCode,
                        ),
                    amountSubtitle = stringResource(R.string.not_included_missing_rate),
                )
                if (index !=
                    state.unconvertedTransactions.lastIndex
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        ScrollToTopButton(
            listState = listState,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun AnalyticsTransaction.originalAmountSubtitle(selectedType: AnalyticsType): String? {
    if (originalAmount.currency == reportingAmount.currency) return null
    val original =
        formatAnalyticsDisplayAmount(
            originalAmount.amount,
            if (category.isIncome) AnalyticsType.Income else AnalyticsType.Expenses,
            selectedType,
            originalAmount.currency.isoCode,
        )
    val formattedDate = rateDate?.format(DateTimeFormatter.ofPattern("dd.MM", Locale.getDefault())) ?: return original
    return stringResource(R.string.original_amount_rate, original, formattedDate)
}

@Composable
internal fun MissingRatesNotice(
    transactions: List<UnconvertedAnalyticsTransaction>,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currencies =
        transactions
            .flatMap { it.missingCurrencies }
            .distinct()
            .sortedBy { it.isoCode }
            .joinToString { it.isoCode }
    Column(
        modifier =
            modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            stringResource(R.string.missing_rates_title),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(R.string.missing_rates_description, transactions.size, currencies),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            stringResource(R.string.retry),
            modifier = Modifier.clickable(role = Role.Button, onClick = onRetry),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
internal fun AnalyticsEmpty(
    state: AnalyticsUiState.Empty,
    onIntent: (AnalyticsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        AnalyticsFilterContent(data = state.data, onIntent = onIntent)
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) { AnalyticsEmptyState() }
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
            contentDescription = null,
            modifier = Modifier.size(200.dp),
        )
        Text(
            stringResource(R.string.empty_period),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.empty_period_description),
            modifier = Modifier.widthIn(max = 280.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TransactionsHeading() =
    Text(
        stringResource(R.string.transactions),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 8.dp),
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 22.sp),
    )

@Composable
internal fun TransactionItem(
    emoji: String,
    title: String,
    comment: String?,
    amount: String,
    modifier: Modifier = Modifier,
    amountSubtitle: String? = null,
) {
    ListItem(modifier = modifier, leadingContent = { EmojiCircle(emoji = emoji) }, content = {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            comment?.takeIf(String::isNotBlank)?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }, trailingContent = {
        Column(horizontalAlignment = Alignment.End) {
            Text(amount, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
            amountSubtitle?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End,
                )
            }
        }
    })
}
