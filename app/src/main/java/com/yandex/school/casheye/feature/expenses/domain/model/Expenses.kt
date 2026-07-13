package com.yandex.school.casheye.feature.expenses.domain.model

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal

data class Expenses(
    val total: BigDecimal,
    val currencyCode: String,
    val transactions: List<Transaction>
)
