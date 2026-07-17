package com.yandex.school.casheye.feature.income.presentation

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal

data class IncomeUiState(
    val total: BigDecimal,
    val currencyCode: String,
    val transactions: List<Transaction>,
)
