package com.yandex.school.casheye.feature.income.domain.model

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal

data class Income(
    val total: BigDecimal,
    val currencyCode: String,
    val transactions: List<Transaction>
)
