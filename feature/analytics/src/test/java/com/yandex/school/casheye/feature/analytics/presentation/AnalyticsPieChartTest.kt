package com.yandex.school.casheye.feature.analytics.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

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
}
