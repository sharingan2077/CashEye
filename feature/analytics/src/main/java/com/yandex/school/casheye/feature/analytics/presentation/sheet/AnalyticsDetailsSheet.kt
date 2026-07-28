package com.yandex.school.casheye.feature.analytics.presentation.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yandex.school.casheye.core.designsystem.component.editor.rememberSheetListGestureCoordinator
import com.yandex.school.casheye.feature.analytics.R
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsIntent
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsType
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsUiState
import com.yandex.school.casheye.feature.analytics.presentation.MissingRatesNotice
import com.yandex.school.casheye.feature.analytics.presentation.chart.AnalyticsPieChart
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsChartPalette
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsColorForCategory
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsColorForType
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsPieChartItems
import com.yandex.school.casheye.feature.analytics.presentation.chart.analyticsTypePieChartItems
import com.yandex.school.casheye.feature.analytics.presentation.formatAnalyticsDisplayAmount
import java.math.BigDecimal
import java.math.RoundingMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsSheet(
    state: AnalyticsUiState.Content,
    onIntent: (AnalyticsIntent) -> Unit,
) {
    val listState = rememberLazyListState()
    val gestureCoordinator = rememberSheetListGestureCoordinator(listState)
    val expensesLabel = stringResource(R.string.type_expenses)
    val incomeLabel = stringResource(R.string.type_income)
    val chartPalette = analyticsChartPalette()
    val isAllTypes = state.data.filters.type == AnalyticsType.All
    val chartItems =
        if (isAllTypes) {
            analyticsTypePieChartItems(
                state.typeSummaries,
                expensesLabel,
                incomeLabel,
                chartPalette,
            )
        } else {
            analyticsPieChartItems(state.categorySummaries, chartPalette)
        }
    val chartTotal =
        if (isAllTypes) {
            state.typeSummaries.fold(BigDecimal.ZERO) { total, summary -> total + summary.amount }
        } else {
            state.total.abs()
        }
    AnalyticsModalBottomSheet(onDismissRequest = { onIntent(AnalyticsIntent.DismissSheet) }) {
        SheetTitle(
            title = stringResource(R.string.details),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            paddingValues = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            isDetails = true,
        )
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 720.dp)
                    .nestedScroll(gestureCoordinator),
            state = listState,
            flingBehavior = gestureCoordinator,
            overscrollEffect = null,
        ) {
            if (state.transactions.isNotEmpty()) {
                item {
                    AnalyticsPieChart(
                        total =
                            formatAnalyticsDisplayAmount(
                                amount = state.total,
                                amountType = AnalyticsType.All,
                                selectedType = state.data.filters.type,
                                currencyCode = state.currencyCode,
                            ),
                        items = chartItems,
                        paddingValues = PaddingValues(bottom = 32.dp),
                        showLegend = false,
                    )
                }
            }
            if (state.unconvertedTransactions.isNotEmpty()) {
                item {
                    MissingRatesNotice(
                        transactions = state.unconvertedTransactions,
                        onRetry = { onIntent(AnalyticsIntent.Retry) },
                        modifier = Modifier.padding(bottom = 24.dp),
                    )
                }
            }
            if (isAllTypes) {
                items(state.typeSummaries, key = { it.type }) { summary ->
                    DetailsSummaryRow(
                        label = if (summary.type == AnalyticsType.Expenses) expensesLabel else incomeLabel,
                        amount = summary.amount,
                        formattedAmount =
                            formatAnalyticsDisplayAmount(
                                amount = summary.amount,
                                amountType = summary.type,
                                selectedType = AnalyticsType.All,
                                currencyCode = state.currencyCode,
                            ),
                        total = chartTotal,
                        color = analyticsColorForType(summary.type, chartPalette),
                    )
                }
            } else {
                items(state.categorySummaries, key = { it.category.id }) { summary ->
                    DetailsSummaryRow(
                        label = summary.category.name,
                        amount = summary.amount,
                        formattedAmount =
                            formatAnalyticsDisplayAmount(
                                amount = summary.amount,
                                amountType = state.data.filters.type,
                                selectedType = state.data.filters.type,
                                currencyCode = state.currencyCode,
                            ),
                        total = chartTotal,
                        color =
                            analyticsColorForCategory(
                                summary.category.id,
                                chartPalette.surface,
                            ),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun DetailsSummaryRow(
    label: String,
    amount: BigDecimal,
    formattedAmount: String,
    total: BigDecimal,
    color: Color,
) {
    val fraction =
        if (total.signum() == 0) {
            BigDecimal.ZERO
        } else {
            amount.divide(total, 4, RoundingMode.HALF_UP)
        }
    val percentage = fraction.movePointRight(2).setScale(1, RoundingMode.HALF_UP)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(
                modifier =
                    Modifier
                        .size(12.dp)
                        .background(color, CircleShape),
            )
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(text = formattedAmount, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "(${percentage.toPlainString()}%)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        val progressShape = RoundedCornerShape(100.dp)
        LinearProgressIndicator(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(progressShape),
            progress = { fraction.toFloat() },
            color = color,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}
