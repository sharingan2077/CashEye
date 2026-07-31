package com.yandex.school.casheye.core.designsystem.component

import com.yandex.school.casheye.core.designsystem.component.money.currencySymbol
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencySymbolTest {
    @Test
    fun `uses symbol for supported currency codes and ISO fallback otherwise`() {
        assertEquals("₽", currencySymbol("RUB"))
        assertEquals("$", currencySymbol("usd"))
        assertEquals("€", currencySymbol("EUR"))
        assertEquals("£", currencySymbol("GBP"))
        assertEquals("¥", currencySymbol("CNY"))
        assertEquals("KZT", currencySymbol("kzt"))
    }
}
