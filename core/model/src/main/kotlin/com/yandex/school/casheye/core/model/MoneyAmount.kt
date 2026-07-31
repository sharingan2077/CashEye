package com.yandex.school.casheye.core.model

import java.math.BigDecimal

data class MoneyAmount(
    val amount: BigDecimal,
    val currency: CurrencyCode,
)
