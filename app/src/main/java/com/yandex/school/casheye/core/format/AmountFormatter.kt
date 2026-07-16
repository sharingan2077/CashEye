package com.yandex.school.casheye.core.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun formatAmount(
    amount: BigDecimal,
    currencyCode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val amountCurrency = Currency.getInstance(currencyCode)

    return NumberFormat
        .getCurrencyInstance(locale)
        .apply {
            currency = amountCurrency
            minimumFractionDigits = 0
            maximumFractionDigits = amountCurrency.defaultFractionDigits
        }.format(amount)
}
