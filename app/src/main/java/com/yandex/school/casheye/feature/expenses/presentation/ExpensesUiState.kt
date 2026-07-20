package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal

data class ExpensesUiState(
    val total: BigDecimal,
    val currencyCode: String,
    val transactions: List<Transaction>,
)
