package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.yandex.school.casheye.feature.analytics.R
import java.math.BigDecimal

private val singleCategoryPlaceholderRatio = BigDecimal("0.000001")
private val otherCategoryColor = Color(0xFF8E8E93)

internal data class AnalyticsPieChartItem(
    val label: String,
    val amount: BigDecimal,
    val color: Color,
)

@Composable
internal fun AnalyticsPieChart(
    total: String,
    items: List<AnalyticsPieChartItem>,
    modifier: Modifier = Modifier,
    paddingValues: PaddingValues = PaddingValues(vertical = 32.dp),
    showLegend: Boolean = true,
    modelProducer: PieChartModelProducer? = null,
    animateIn: Boolean = true,
    onChartDispose: (() -> Unit)? = null,
) {
    val internalModelProducer = remember { PieChartModelProducer() }
    val chartModelProducer = modelProducer ?: internalModelProducer
    val colors = items.map(AnalyticsPieChartItem::color)
    val chartColors = if (items.size == 1) colors + Color.Transparent else colors
    if (modelProducer == null) {
        LaunchedEffect(items) {
            chartModelProducer.runTransaction { pieSeries { series(analyticsPieChartValues(items)) } }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(paddingValues),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PieChartWithTotal(
            total = total,
            colors = chartColors,
            modelProducer = chartModelProducer,
            animateIn = animateIn,
            onChartDispose = onChartDispose,
        )
        if (showLegend) AnalyticsLegend(items = items)
    }
}

@Composable
private fun PieChartWithTotal(
    total: String,
    colors: List<Color>,
    modelProducer: PieChartModelProducer,
    animateIn: Boolean,
    onChartDispose: (() -> Unit)?,
) {
    DisposableEffect(onChartDispose) {
        onDispose { onChartDispose?.invoke() }
    }
    Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        PieChartHost(
            chart =
                rememberPieChart(
                    sliceProvider =
                        PieChart.SliceProvider.series(
                            colors.map { color -> PieChart.Slice(fill = Fill(color)) },
                        ),
                    innerSize = PieSize.Inner.fixed(192.dp),
                ),
            modelProducer = modelProducer,
            modifier = Modifier.size(240.dp),
            animateIn = animateIn,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.total_for_period),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelLarge,
            )
            Text(text = total, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun AnalyticsLegend(
    items: List<AnalyticsPieChartItem>,
) {
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(
                    modifier =
                        Modifier
                            .size(12.dp)
                            .background(item.color, CircleShape),
                )
                Text(
                    text = item.label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

internal fun analyticsColorForCategory(categoryId: Int): Color {
    val normalizedId = categoryId.toLong() and UINT_MASK
    val hue = (normalizedId.rem(COLOR_SEQUENCE_LENGTH).toFloat() * GOLDEN_ANGLE).rem(FULL_HUE)
    return Color.hsv(hue = hue, saturation = 0.58f, value = 0.94f)
}

internal fun analyticsPieChartItems(categories: List<AnalyticsCategorySummary>): List<AnalyticsPieChartItem> =
    categories.map { summary ->
        AnalyticsPieChartItem(
            label = summary.category.name,
            amount = summary.amount,
            color = analyticsColorForCategory(summary.category.id),
        )
    }

internal fun analyticsOverviewPieChartItems(
    categories: List<AnalyticsCategorySummary>,
    otherLabel: String,
): List<AnalyticsPieChartItem> {
    val leadingItems = analyticsPieChartItems(categories.take(MAX_OVERVIEW_CATEGORIES))
    if (categories.size <= MAX_OVERVIEW_CATEGORIES) return leadingItems

    val otherAmount =
        categories
            .drop(MAX_OVERVIEW_CATEGORIES)
            .fold(BigDecimal.ZERO) { total, summary -> total + summary.amount }
    return leadingItems +
        AnalyticsPieChartItem(
            label = otherLabel,
            amount = otherAmount,
            color = otherCategoryColor,
        )
}

internal fun analyticsPieChartValues(items: List<AnalyticsPieChartItem>): List<BigDecimal> {
    val amounts = items.map(AnalyticsPieChartItem::amount)
    return if (amounts.size == 1) {
        amounts + amounts.single().multiply(singleCategoryPlaceholderRatio)
    } else {
        amounts
    }
}

private const val UINT_MASK = 0xffffffffL
private const val COLOR_SEQUENCE_LENGTH = 1_000_003L
private const val FULL_HUE = 360L
private const val GOLDEN_ANGLE = 137.508f
private const val MAX_OVERVIEW_CATEGORIES = 4
