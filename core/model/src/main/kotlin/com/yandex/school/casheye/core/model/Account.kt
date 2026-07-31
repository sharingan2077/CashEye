package com.yandex.school.casheye.core.model

import java.math.BigDecimal

data class Account(
    val id: Int,
    val name: String,
    val emoji: String,
    val balance: BigDecimal,
    val currency: CurrencyCode,
) {
    constructor(
        id: Int,
        name: String,
        emoji: String,
        balance: BigDecimal,
        currency: String,
    ) : this(id, name, emoji, balance, CurrencyCode.fromIsoCode(currency))
}
