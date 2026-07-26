package com.yandex.school.casheye.core.model

import java.util.Locale

enum class CurrencyCode(
    val isoCode: String,
) {
    RUB("RUB"),
    USD("USD"),
    EUR("EUR"),
    GBP("GBP"),
    CNY("CNY"),
    ;

    companion object {
        fun fromIsoCode(value: String): CurrencyCode =
            entries.firstOrNull { it.isoCode == value.uppercase(Locale.ROOT) }
                ?: throw IllegalArgumentException("Unsupported currency code: $value")

        fun fromIsoCodeOrNull(value: String?): CurrencyCode? =
            value?.let { code -> entries.firstOrNull { it.isoCode == code.uppercase(Locale.ROOT) } }
    }
}
