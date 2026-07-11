package com.yandex.school.casheye.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.math.BigDecimal

class AmountFormatterTest {

    @Test
    fun `formats amounts using Russian separators and currency symbol`() {
        val formattedAmount = formatAmount(
            amount = BigDecimal("1234567.89"),
            currencyCode = "USD",
        )

        assertEquals("1\u00A0234\u00A0567,89\u00A0$", formattedAmount)
    }

    @Test
    fun `omits trailing fractional zeroes`() {
        val formattedAmount = formatAmount(
            amount = BigDecimal("10.50"),
            currencyCode = "USD",
        )

        assertEquals("10,5\u00A0$", formattedAmount)
    }

    @Test
    fun `rejects unsupported currency codes`() {
        assertThrows(IllegalArgumentException::class.java) {
            formatAmount(BigDecimal.ONE, "INVALID")
        }
    }
}
