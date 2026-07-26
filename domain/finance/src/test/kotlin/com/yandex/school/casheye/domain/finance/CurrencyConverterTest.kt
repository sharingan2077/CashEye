package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class CurrencyConverterTest {
    private val converter = CurrencyConverter()
    private val friday = LocalDate.of(2026, 7, 24)
    private val saturday = friday.plusDays(1)
    private val rates =
        listOf(
            rate(CurrencyCode.USD, "1.2", friday),
            rate(CurrencyCode.GBP, "0.8", friday),
            rate(CurrencyCode.RUB, "90", friday),
            rate(CurrencyCode.CNY, "8", friday),
        )

    @Test
    fun `cross rate divides target EUR rate by source EUR rate`() {
        val result = converter.convert(MoneyAmount(BigDecimal("12"), CurrencyCode.USD), CurrencyCode.GBP, friday, rates)

        val complete = result as CurrencyConversionResult.Complete
        assertEquals(0, complete.money.amount.compareTo(BigDecimal("8")))
        assertEquals(CurrencyCode.GBP, complete.money.currency)
    }

    @Test
    fun `all supported currency pairs can be converted`() {
        CurrencyCode.entries.forEach { source ->
            CurrencyCode.entries.forEach { target ->
                val result = converter.convert(MoneyAmount(BigDecimal.ONE, source), target, friday, rates)

                assertTrue("$source to $target must be complete", result is CurrencyConversionResult.Complete)
            }
        }
    }

    @Test
    fun `identity conversion does not require provider rate`() {
        val result =
            converter.convert(
                MoneyAmount(BigDecimal("42.50"), CurrencyCode.CNY),
                CurrencyCode.CNY,
                saturday,
                emptyList(),
            )

        assertEquals(BigDecimal("42.50"), (result as CurrencyConversionResult.Complete).money.amount)
    }

    @Test
    fun `weekend conversion uses latest rate not after requested date`() {
        val result = converter.convert(MoneyAmount(BigDecimal.ONE, CurrencyCode.EUR), CurrencyCode.RUB, saturday, rates)

        val complete = result as CurrencyConversionResult.Complete
        assertEquals(friday, complete.targetRateDate)
        assertEquals(0, complete.money.amount.compareTo(BigDecimal("90")))
    }

    @Test
    fun `missing rate is explicit`() {
        val result =
            converter.convert(
                MoneyAmount(BigDecimal.ONE, CurrencyCode.USD),
                CurrencyCode.GBP,
                friday,
                emptyList(),
            )

        assertTrue(result is CurrencyConversionResult.Incomplete)
        assertEquals(
            setOf(CurrencyCode.USD, CurrencyCode.GBP),
            (result as CurrencyConversionResult.Incomplete).missingCurrencies,
        )
    }

    private fun rate(
        quote: CurrencyCode,
        value: String,
        date: LocalDate,
    ) = ExchangeRate(CurrencyCode.EUR, quote, BigDecimal(value), date)
}
