package com.yandex.school.casheye.feature.analytics.presentation.chart

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.yandex.school.casheye.core.designsystem.theme.CashEyeExtendedTheme
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsCategorySummary
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsType
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsTypeSummary
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
        extendedColors.chartExpense,
        extendedColors.chartIncome,
        extendedColors.chartOther,
        MaterialTheme.colorScheme.surface,
    )
}

internal fun analyticsColorForCategory(
    categoryId: Int,
    surface: Color,
): Color {
    val pair =
        CATEGORY_COLOR_PAIRS[
            Math
                .floorMod(
                    categoryId.toLong() - FIRST_CATEGORY_ID,
                    CATEGORY_COLOR_PAIRS.size.toLong(),
                ).toInt(),
        ]
    return if (surface.luminance() >= LIGHT_SURFACE_LUMINANCE) pair.light else pair.dark
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
    summaries
        .sortedByDescending {
            it.amount.abs()
        }.map { summary ->
            AnalyticsPieChartItem(
                when (summary.type) {
                    AnalyticsType.Expenses -> expensesLabel
                    AnalyticsType.Income -> incomeLabel
                    AnalyticsType.All -> error("All is not a chart group")
                },
                summary.amount.abs(),
                analyticsColorForType(summary.type, palette),
            )
        }

internal fun analyticsPieChartItems(
    categories: List<AnalyticsCategorySummary>,
    palette: AnalyticsChartPalette,
): List<AnalyticsPieChartItem> =
    categories.map {
        AnalyticsPieChartItem(it.category.name, it.amount, analyticsColorForCategory(it.category.id, palette.surface))
    }

internal fun analyticsOverviewPieChartItems(
    categories: List<AnalyticsCategorySummary>,
    otherLabel: String,
    palette: AnalyticsChartPalette,
): List<AnalyticsPieChartItem> {
    val leadingItems = analyticsPieChartItems(categories.take(MAX_OVERVIEW_CATEGORIES), palette)
    if (categories.size <= MAX_OVERVIEW_CATEGORIES) return leadingItems
    val otherAmount =
        categories.drop(MAX_OVERVIEW_CATEGORIES).fold(BigDecimal.ZERO) { total, summary ->
            total +
                summary.amount
        }
    return leadingItems + AnalyticsPieChartItem(otherLabel, otherAmount, palette.other)
}

internal fun contrastRatio(
    first: Color,
    second: Color,
): Float =
    (maxOf(first.luminance(), second.luminance()) + CONTRAST_LUMINANCE_OFFSET) /
        (minOf(first.luminance(), second.luminance()) + CONTRAST_LUMINANCE_OFFSET)

internal fun analyticsPieChartValues(items: List<AnalyticsPieChartItem>): List<BigDecimal> =
    items.map(AnalyticsPieChartItem::amount).let {
        if (it.size ==
            1
        ) {
            it + it.single().multiply(singleCategoryPlaceholderRatio)
        } else {
            it
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
        AnalyticsCategoryColorPair(Color(0xFFABE016), Color(0xFFA2DB02)),
        AnalyticsCategoryColorPair(Color(0xFFB5A2FE), Color(0xFF9A83F7)),
        AnalyticsCategoryColorPair(Color(0xFF40E0B0), Color(0xFF20CA99)),
        AnalyticsCategoryColorPair(Color(0xFFFFD485), Color(0xFFFBBC3B)),
        AnalyticsCategoryColorPair(Color(0xFFFF9FCA), Color(0xFFF66AAD)),
        AnalyticsCategoryColorPair(Color(0xFF79CDF7), Color(0xFF42B7EF)),
        AnalyticsCategoryColorPair(Color(0xFFFF9A8B), Color(0xFFF56F61)),
        AnalyticsCategoryColorPair(Color(0xFF62D7E5), Color(0xFF28BFCE)),
        AnalyticsCategoryColorPair(Color(0xFF8CB4FF), Color(0xFF5B92F5)),
        AnalyticsCategoryColorPair(Color(0xFFFFB36B), Color(0xFFF58D32)),
        AnalyticsCategoryColorPair(Color(0xFFD296FF), Color(0xFFBB62F4)),
        AnalyticsCategoryColorPair(Color(0xFF73D997), Color(0xFF42C674)),
        AnalyticsCategoryColorPair(Color(0xFFF29ADF), Color(0xFFDE64C4)),
        AnalyticsCategoryColorPair(Color(0xFF9CA7FF), Color(0xFF727FF2)),
        AnalyticsCategoryColorPair(Color(0xFF65D8C9), Color(0xFF2FC2B2)),
        AnalyticsCategoryColorPair(Color(0xFFFF9BA5), Color(0xFFF26A78)),
        AnalyticsCategoryColorPair(Color(0xFFC7E86B), Color(0xFFA9D63E)),
        AnalyticsCategoryColorPair(Color(0xFFA7C5FF), Color(0xFF719CEF)),
        AnalyticsCategoryColorPair(Color(0xFFFFB0E6), Color(0xFFF27BCB)),
        AnalyticsCategoryColorPair(Color(0xFF7DE3A1), Color(0xFF43CD77)),
        AnalyticsCategoryColorPair(Color(0xFFC4A6FF), Color(0xFFA274F5)),
        AnalyticsCategoryColorPair(Color(0xFFFFCA9E), Color(0xFFF5A45F)),
        AnalyticsCategoryColorPair(Color(0xFF6ED9F2), Color(0xFF32BFD9)),
        AnalyticsCategoryColorPair(Color(0xFFF5A0B8), Color(0xFFE86B91)),
    )
