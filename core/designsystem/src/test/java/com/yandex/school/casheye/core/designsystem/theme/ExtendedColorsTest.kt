package com.yandex.school.casheye.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtendedColorsTest {
    @Test
    fun `light and dark chart colors use theme specific tones`() {
        assertEquals(Color(0xFFC43E3E), LightExtendedColors.chartExpense)
        assertEquals(Color(0xFF329E5D), LightExtendedColors.chartIncome)
        assertEquals(Color(0xFFC65A77), LightExtendedColors.chartOther)
        assertEquals(Color(0xFFFF8585), DarkExtendedColors.chartExpense)
        assertEquals(Color(0xFF72DB9C), DarkExtendedColors.chartIncome)
        assertEquals(Color(0xFFFF9EB8), DarkExtendedColors.chartOther)
    }

    @Test
    fun `chart colors meet non text contrast in both themes`() {
        assertPaletteContrast(LightExtendedColors, LightSurface)
        assertPaletteContrast(DarkExtendedColors, DarkSurface)
    }

    @Test
    fun `inverse surface pairs keep readable confirm content`() {
        assertTrue(contrastRatio(LightInverseOnSurface, LightInverseSurface) >= MIN_CONTENT_CONTRAST)
        assertTrue(contrastRatio(DarkInverseOnSurface, DarkInverseSurface) >= MIN_CONTENT_CONTRAST)
    }

    private fun assertPaletteContrast(
        colors: CashEyeExtendedColors,
        surface: Color,
    ) {
        listOf(colors.chartExpense, colors.chartIncome, colors.chartOther).forEach { color ->
            assertTrue(
                "Expected $color to contrast with $surface",
                contrastRatio(color, surface) >= MIN_CHART_CONTRAST,
            )
        }
    }

    private fun contrastRatio(
        first: Color,
        second: Color,
    ): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private companion object {
        const val MIN_CHART_CONTRAST = 3f
        const val MIN_CONTENT_CONTRAST = 4.5f
        const val LUMINANCE_OFFSET = 0.05f
    }
}
