package com.yandex.school.casheye.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CurrencyCodeTest {
    @Test
    fun `parses every supported ISO code`() {
        CurrencyCode.entries.forEach { currency ->
            assertEquals(currency, CurrencyCode.fromIsoCode(currency.isoCode))
        }
    }

    @Test
    fun `returns null for an unsupported ISO code`() {
        assertNull(CurrencyCode.fromIsoCodeOrNull("JPY"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects an unsupported ISO code`() {
        CurrencyCode.fromIsoCode("JPY")
    }
}
