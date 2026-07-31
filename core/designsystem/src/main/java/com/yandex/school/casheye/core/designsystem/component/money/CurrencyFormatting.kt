package com.yandex.school.casheye.core.designsystem.component.money

import java.util.Locale

/** Returns a compact symbol for amounts while retaining an ISO fallback for unknown currencies. */
fun currencySymbol(currencyCode: String): String =
    when (currencyCode.uppercase(Locale.ROOT)) {
        "RUB" -> "₽"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "CNY" -> "¥"
        else -> currencyCode.uppercase(Locale.ROOT)
    }
