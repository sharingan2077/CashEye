package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal

sealed interface ExpensesUiState {
    data object Loading : ExpensesUiState

    data object Empty : ExpensesUiState

    data class Content(
        val total: BigDecimal,
        val currencyCode: String,
        val transactions: List<Transaction>,
    ) : ExpensesUiState

    data class Error(
        val message: String,
    ) : ExpensesUiState
}

sealed interface ExpensesIntent {
    data object Retry : ExpensesIntent
}

sealed interface ExpensesEffect {
    data class ShowError(
        val message: String,
    ) : ExpensesEffect
}
