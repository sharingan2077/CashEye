package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.ui.graphics.Color
import com.yandex.school.casheye.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class AnalyticsPieChartTest {
    @Test
    fun `category color is stable for the same id`() {
        assertEquals(
            analyticsColorForCategory(42, LIGHT_PALETTE.surface),
            analyticsColorForCategory(42, LIGHT_PALETTE.surface),
        )
    }

    @Test
    fun `category color adapts to active surface`() {
        assertNotEquals(
            analyticsColorForCategory(42, LIGHT_PALETTE.surface),
            analyticsColorForCategory(42, DARK_PALETTE.surface),
        )
    }

    @Test
    fun `first twenty categories receive distinct colors in both themes`() {
        val lightColors = (0 until 20).map { analyticsColorForCategory(it, LIGHT_PALETTE.surface) }
        val darkColors = (0 until 20).map { analyticsColorForCategory(it, DARK_PALETTE.surface) }

        assertEquals(lightColors.size, lightColors.toSet().size)
        assertEquals(darkColors.size, darkColors.toSet().size)
    }

    @Test
    fun `extended category range does not repeat colors in both themes`() {
        val lightColors = (0 until 100).map { analyticsColorForCategory(it, LIGHT_PALETTE.surface) }
        val darkColors = (0 until 100).map { analyticsColorForCategory(it, DARK_PALETTE.surface) }

        assertEquals(lightColors.size, lightColors.toSet().size)
        assertEquals(darkColors.size, darkColors.toSet().size)
    }

    @Test
    fun `distant category ids receive different colors`() {
        assertNotEquals(
            analyticsColorForCategory(3, LIGHT_PALETTE.surface),
            analyticsColorForCategory(23, LIGHT_PALETTE.surface),
        )
    }

    @Test
    fun `category colors keep non text contrast against both surfaces`() {
        listOf(LIGHT_PALETTE, DARK_PALETTE).forEach { palette ->
            (0 until 100).forEach { categoryId ->
                val color = analyticsColorForCategory(categoryId, palette.surface)
                assertContrastAtLeast(color, palette.surface, MIN_CHART_CONTRAST)
            }
        }
    }

    @Test
    fun `four categories remain separate in overview chart`() {
        val categories = categorySummaries("40", "30", "20", "10")

        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL, LIGHT_PALETTE)

        assertEquals(listOf("Category 1", "Category 2", "Category 3", "Category 4"), items.map { it.label })
        assertEquals(categories.map { it.amount }, items.map { it.amount })
    }

    @Test
    fun `fifth category becomes other in overview chart`() {
        val categories = categorySummaries("50", "40", "30", "20", "10")

        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL, LIGHT_PALETTE)

        assertEquals(
            listOf("Category 1", "Category 2", "Category 3", "Category 4", OTHER_LABEL),
            items.map { it.label },
        )
        assertEquals(BigDecimal("10"), items.last().amount)
    }

    @Test
    fun `categories after fourth are summed without changing total`() {
        val categories = categorySummaries("60", "50", "40", "30", "20", "10")

        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL, LIGHT_PALETTE)

        assertEquals(BigDecimal("30"), items.last().amount)
        assertEquals(
            categories.fold(BigDecimal.ZERO) { total, summary -> total + summary.amount },
            items.fold(BigDecimal.ZERO) { total, item -> total + item.amount },
        )
    }

    @Test
    fun `full chart keeps every category separate`() {
        val categories = categorySummaries("60", "50", "40", "30", "20", "10")

        val items = analyticsPieChartItems(categories, LIGHT_PALETTE)

        assertEquals(categories.size, items.size)
        assertEquals(categories.map { it.category.name }, items.map { it.label })
        assertEquals(categories.map { it.amount }, items.map { it.amount })
    }

    @Test
    fun `other uses a color different from leading categories`() {
        val categories = categorySummaries("50", "40", "30", "20", "10")

        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL, LIGHT_PALETTE)

        items.dropLast(1).forEach { item -> assertNotEquals(item.color, items.last().color) }
    }

    @Test
    fun `type and other colors use the supplied theme palette`() {
        assertEquals(LIGHT_PALETTE.expense, analyticsColorForType(AnalyticsType.Expenses, LIGHT_PALETTE))
        assertEquals(LIGHT_PALETTE.income, analyticsColorForType(AnalyticsType.Income, LIGHT_PALETTE))

        val categories = categorySummaries("50", "40", "30", "20", "10")
        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL, DARK_PALETTE)

        assertEquals(DARK_PALETTE.other, items.last().color)
    }

    @Test
    fun `type chart keeps only existing positive groups`() {
        val items =
            analyticsTypePieChartItems(
                summaries =
                    listOf(
                        AnalyticsTypeSummary(AnalyticsType.Expenses, BigDecimal("40")),
                        AnalyticsTypeSummary(AnalyticsType.Income, BigDecimal("60")),
                    ),
                expensesLabel = EXPENSES_LABEL,
                incomeLabel = INCOME_LABEL,
                palette = LIGHT_PALETTE,
            )

        assertEquals(listOf(INCOME_LABEL, EXPENSES_LABEL), items.map { it.label })
        assertEquals(listOf(BigDecimal("60"), BigDecimal("40")), items.map { it.amount })
    }

    @Test
    fun `type chart keeps a single existing group`() {
        val items =
            analyticsTypePieChartItems(
                summaries = listOf(AnalyticsTypeSummary(AnalyticsType.Income, BigDecimal("60"))),
                expensesLabel = EXPENSES_LABEL,
                incomeLabel = INCOME_LABEL,
                palette = LIGHT_PALETTE,
            )

        assertEquals(listOf(INCOME_LABEL), items.map { it.label })
        assertEquals(listOf(BigDecimal("60")), items.map { it.amount })
    }

    private fun categorySummaries(vararg amounts: String): List<AnalyticsCategorySummary> =
        amounts.mapIndexed { index, amount ->
            AnalyticsCategorySummary(
                category =
                    Category(
                        id = index + 1,
                        name = "Category ${index + 1}",
                        emoji = "",
                        isIncome = false,
                    ),
                amount = BigDecimal(amount),
            )
        }

    private fun assertContrastAtLeast(
        foreground: Color,
        background: Color,
        expected: Float,
    ) {
        val actual = contrastRatio(foreground, background)
        assertTrue(
            "Expected contrast >= $expected, actual=$actual, foreground=$foreground, background=$background",
            actual >= expected,
        )
    }

    private companion object {
        const val OTHER_LABEL = "Other"
        const val EXPENSES_LABEL = "Expenses"
        const val INCOME_LABEL = "Income"
        const val MIN_CHART_CONTRAST = 3f
        val LIGHT_PALETTE =
            AnalyticsChartPalette(
                expense = Color(0xFFC43E3E),
                income = Color(0xFF329E5D),
                other = Color(0xFFC65A77),
                surface = Color(0xFFFEF7FF),
            )
        val DARK_PALETTE =
            AnalyticsChartPalette(
                expense = Color(0xFFFF8585),
                income = Color(0xFF72DB9C),
                other = Color(0xFFFF9EB8),
                surface = Color(0xFF141218),
            )
    }
}
