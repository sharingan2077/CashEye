package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import java.math.BigDecimal

internal fun aggregateNativeMoney(
    amounts: Iterable<MoneyAmount>,
    reportingCurrency: CurrencyCode,
): List<MoneyAmount> {
    val totals =
        amounts
            .groupingBy(MoneyAmount::currency)
            .fold(BigDecimal.ZERO) { total, money -> total + money.amount }

    return CurrencyCode.entries
        .sortedWith(compareBy<CurrencyCode> { it != reportingCurrency }.thenBy { it.ordinal })
        .mapNotNull { currency ->
            totals[currency]
                ?.takeUnless { it.compareTo(BigDecimal.ZERO) == 0 }
                ?.let { MoneyAmount(it, currency) }
        }
}
