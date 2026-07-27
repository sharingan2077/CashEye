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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.pie.PieChart
import com.patrykandpatrick.vico.compose.pie.PieChartHost
import com.patrykandpatrick.vico.compose.pie.PieSize
import com.patrykandpatrick.vico.compose.pie.data.PieChartModelProducer
import com.patrykandpatrick.vico.compose.pie.data.pieSeries
import com.patrykandpatrick.vico.compose.pie.rememberPieChart
import com.yandex.school.casheye.core.designsystem.theme.CashEyeExtendedTheme
import com.yandex.school.casheye.feature.analytics.R
import java.math.BigDecimal

private val singleCategoryPlaceholderRatio = BigDecimal("0.000001")

internal data class AnalyticsChartPalette(
    val expense: Color,
    val income: Color,
    val other: Color,
    val surface: Color,
)

internal data class AnalyticsPieChartItem(
    val label: String,
    val amount: BigDecimal,
    val color: Color,
)

@Composable
internal fun analyticsChartPalette(): AnalyticsChartPalette {
    val extendedColors = CashEyeExtendedTheme.colors
    return AnalyticsChartPalette(
        expense = extendedColors.chartExpense,
        income = extendedColors.chartIncome,
        other = extendedColors.chartOther,
        surface = MaterialTheme.colorScheme.surface,
    )
}

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
private fun AnalyticsLegend(items: List<AnalyticsPieChartItem>) {
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

internal fun analyticsColorForCategory(
    categoryId: Int,
    surface: Color,
): Color {
    val lightSurface = surface.luminance() >= LIGHT_SURFACE_LUMINANCE
    val paletteIndex =
        Math.floorMod(
            categoryId.toLong() - FIRST_CATEGORY_ID,
            CATEGORY_COLOR_PAIRS.size.toLong(),
        ).toInt()
    val pair = CATEGORY_COLOR_PAIRS[paletteIndex]
    return if (lightSurface) pair.light else pair.dark
}

internal fun analyticsColorForType(
    type: AnalyticsType,
    palette: AnalyticsChartPalette,
): Color =
    when (type) {
        AnalyticsType.Expenses -> palette.expense
        AnalyticsType.Income -> palette.income
        AnalyticsType.All -> error("All is not a chart group")
    }

internal fun analyticsTypePieChartItems(
    summaries: List<AnalyticsTypeSummary>,
    expensesLabel: String,
    incomeLabel: String,
    palette: AnalyticsChartPalette,
): List<AnalyticsPieChartItem> =
    summaries.sortedByDescending { it.amount.abs() }.map { summary ->
        AnalyticsPieChartItem(
            label =
                when (summary.type) {
                    AnalyticsType.Expenses -> expensesLabel
                    AnalyticsType.Income -> incomeLabel
                    AnalyticsType.All -> error("All is not a chart group")
                },
            amount = summary.amount.abs(),
            color = analyticsColorForType(summary.type, palette),
        )
    }

internal fun analyticsPieChartItems(
    categories: List<AnalyticsCategorySummary>,
    palette: AnalyticsChartPalette,
): List<AnalyticsPieChartItem> =
    categories.map { summary ->
        AnalyticsPieChartItem(
            label = summary.category.name,
            amount = summary.amount,
            color = analyticsColorForCategory(summary.category.id, palette.surface),
        )
    }

internal fun analyticsOverviewPieChartItems(
    categories: List<AnalyticsCategorySummary>,
    otherLabel: String,
    palette: AnalyticsChartPalette,
): List<AnalyticsPieChartItem> {
    val leadingItems = analyticsPieChartItems(categories.take(MAX_OVERVIEW_CATEGORIES), palette)
    if (categories.size <= MAX_OVERVIEW_CATEGORIES) return leadingItems

    val otherAmount =
        categories
            .drop(MAX_OVERVIEW_CATEGORIES)
            .fold(BigDecimal.ZERO) { total, summary -> total + summary.amount }
    return leadingItems +
        AnalyticsPieChartItem(
            label = otherLabel,
            amount = otherAmount,
            color = palette.other,
        )
}

internal fun contrastRatio(
    first: Color,
    second: Color,
): Float {
    val lighter = maxOf(first.luminance(), second.luminance())
    val darker = minOf(first.luminance(), second.luminance())
    return (lighter + CONTRAST_LUMINANCE_OFFSET) / (darker + CONTRAST_LUMINANCE_OFFSET)
}

internal fun analyticsPieChartValues(items: List<AnalyticsPieChartItem>): List<BigDecimal> {
    val amounts = items.map(AnalyticsPieChartItem::amount)
    return if (amounts.size == 1) {
        amounts + amounts.single().multiply(singleCategoryPlaceholderRatio)
    } else {
        amounts
    }
}

private const val MAX_OVERVIEW_CATEGORIES = 4
private const val FIRST_CATEGORY_ID = 1L
private const val LIGHT_SURFACE_LUMINANCE = 0.5f
private const val CONTRAST_LUMINANCE_OFFSET = 0.05f

private data class AnalyticsCategoryColorPair(
    val light: Color,
    val dark: Color,
)

private val CATEGORY_COLOR_PAIRS =
    listOf(
        AnalyticsCategoryColorPair(light = Color(0xFFABE016), dark = Color(0xFFA2DB02)),
        AnalyticsCategoryColorPair(light = Color(0xFFB5A2FE), dark = Color(0xFF9A83F7)),
        AnalyticsCategoryColorPair(light = Color(0xFF40E0B0), dark = Color(0xFF20CA99)),
        AnalyticsCategoryColorPair(light = Color(0xFFFFD485), dark = Color(0xFFFBBC3B)),
        AnalyticsCategoryColorPair(light = Color(0xFFFF9FCA), dark = Color(0xFFF66AAD)),
        AnalyticsCategoryColorPair(light = Color(0xFF79CDF7), dark = Color(0xFF42B7EF)),
        AnalyticsCategoryColorPair(light = Color(0xFFFF9A8B), dark = Color(0xFFF56F61)),
        AnalyticsCategoryColorPair(light = Color(0xFF62D7E5), dark = Color(0xFF28BFCE)),
        AnalyticsCategoryColorPair(light = Color(0xFF8CB4FF), dark = Color(0xFF5B92F5)),
        AnalyticsCategoryColorPair(light = Color(0xFFFFB36B), dark = Color(0xFFF58D32)),
        AnalyticsCategoryColorPair(light = Color(0xFFD296FF), dark = Color(0xFFBB62F4)),
        AnalyticsCategoryColorPair(light = Color(0xFF73D997), dark = Color(0xFF42C674)),
        AnalyticsCategoryColorPair(light = Color(0xFFF29ADF), dark = Color(0xFFDE64C4)),
        AnalyticsCategoryColorPair(light = Color(0xFF9CA7FF), dark = Color(0xFF727FF2)),
        AnalyticsCategoryColorPair(light = Color(0xFF65D8C9), dark = Color(0xFF2FC2B2)),
        AnalyticsCategoryColorPair(light = Color(0xFFFF9BA5), dark = Color(0xFFF26A78)),
        AnalyticsCategoryColorPair(light = Color(0xFFC7E86B), dark = Color(0xFFA9D63E)),
        AnalyticsCategoryColorPair(light = Color(0xFFA7C5FF), dark = Color(0xFF719CEF)),
        AnalyticsCategoryColorPair(light = Color(0xFFFFB0E6), dark = Color(0xFFF27BCB)),
        AnalyticsCategoryColorPair(light = Color(0xFF7DE3A1), dark = Color(0xFF43CD77)),
        AnalyticsCategoryColorPair(light = Color(0xFFC4A6FF), dark = Color(0xFFA274F5)),
        AnalyticsCategoryColorPair(light = Color(0xFFFFCA9E), dark = Color(0xFFF5A45F)),
        AnalyticsCategoryColorPair(light = Color(0xFF6ED9F2), dark = Color(0xFF32BFD9)),
        AnalyticsCategoryColorPair(light = Color(0xFFF5A0B8), dark = Color(0xFFE86B91)),
    )
