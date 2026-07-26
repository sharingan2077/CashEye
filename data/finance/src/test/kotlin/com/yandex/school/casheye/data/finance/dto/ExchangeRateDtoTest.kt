package com.yandex.school.casheye.data.finance.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ExchangeRateDtoTest {
    @Test
    fun `parses Frankfurter v2 flat time series fixture`() {
        val fixture =
            """
            [
              {"date":"2026-07-24","base":"EUR","quote":"USD","rate":1.1742},
              {"date":"2026-07-24","base":"EUR","quote":"GBP","rate":0.87345}
            ]
            """.trimIndent()

        val rates = Json.decodeFromString<List<ExchangeRateDto>>(fixture)

        assertEquals(LocalDate.of(2026, 7, 24), rates.first().date)
        assertEquals("EUR", rates.first().base)
        assertEquals("USD", rates.first().quote)
        assertEquals(0, rates.first().rate.compareTo(BigDecimal("1.1742")))
    }
}
