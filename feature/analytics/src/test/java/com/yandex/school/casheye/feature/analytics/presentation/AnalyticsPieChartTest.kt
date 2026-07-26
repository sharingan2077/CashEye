package com.yandex.school.casheye.feature.analytics.presentation

import com.yandex.school.casheye.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.math.BigDecimal

class AnalyticsPieChartTest {
    @Test
    fun `category color is stable for the same id`() {
        assertEquals(analyticsColorForCategory(42), analyticsColorForCategory(42))
    }

    @Test
    fun `first twenty categories receive distinct colors`() {
        val colors = (0 until 20).map(::analyticsColorForCategory)

        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun `extended category range does not repeat colors`() {
        val colors = (0 until 100).map(::analyticsColorForCategory)

        assertEquals(colors.size, colors.toSet().size)
    }

    @Test
    fun `distant category ids receive different colors`() {
        assertNotEquals(analyticsColorForCategory(3), analyticsColorForCategory(23))
    }

    @Test
    fun `four categories remain separate in overview chart`() {
        val categories = categorySummaries("40", "30", "20", "10")

        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL)

        assertEquals(listOf("Category 1", "Category 2", "Category 3", "Category 4"), items.map { it.label })
        assertEquals(categories.map { it.amount }, items.map { it.amount })
    }

    @Test
    fun `fifth category becomes other in overview chart`() {
        val categories = categorySummaries("50", "40", "30", "20", "10")

        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL)

        assertEquals(
            listOf("Category 1", "Category 2", "Category 3", "Category 4", OTHER_LABEL),
            items.map { it.label },
        )
        assertEquals(BigDecimal("10"), items.last().amount)
    }

    @Test
    fun `categories after fourth are summed without changing total`() {
        val categories = categorySummaries("60", "50", "40", "30", "20", "10")

        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL)

        assertEquals(BigDecimal("30"), items.last().amount)
        assertEquals(
            categories.fold(BigDecimal.ZERO) { total, summary -> total + summary.amount },
            items.fold(BigDecimal.ZERO) { total, item -> total + item.amount },
        )
    }

    @Test
    fun `full chart keeps every category separate`() {
        val categories = categorySummaries("60", "50", "40", "30", "20", "10")

        val items = analyticsPieChartItems(categories)

        assertEquals(categories.size, items.size)
        assertEquals(categories.map { it.category.name }, items.map { it.label })
        assertEquals(categories.map { it.amount }, items.map { it.amount })
    }

    @Test
    fun `other uses a color different from leading categories`() {
        val categories = categorySummaries("50", "40", "30", "20", "10")

        val items = analyticsOverviewPieChartItems(categories, OTHER_LABEL)

        items.dropLast(1).forEach { item -> assertNotEquals(item.color, items.last().color) }
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

    private companion object {
        const val OTHER_LABEL = "Other"
    }
}
