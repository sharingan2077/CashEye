package com.yandex.school.casheye.core.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val RussianLocale = Locale.forLanguageTag("ru-RU")

fun formatAmount(
    amount: BigDecimal,
    currencyCode: String,
): String {
    val amountCurrency = Currency.getInstance(currencyCode)

    return NumberFormat
        .getCurrencyInstance(RussianLocale)
        .apply {
            currency = amountCurrency
            minimumFractionDigits = 0
            maximumFractionDigits = amountCurrency.defaultFractionDigits
        }
        .format(amount)
}
