package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import java.math.BigDecimal
import java.time.LocalDate

sealed interface ExpensesUiState {
    data object Loading : ExpensesUiState

    data object Empty : ExpensesUiState

    data class Content(
        val total: BigDecimal,
        val currencyCode: String,
        val transactions: List<Transaction>,
    ) : ExpensesUiState

    data class Error(
        val reason: FinanceFailureReason,
    ) : ExpensesUiState
}

sealed interface ExpensesIntent {
    data object Retry : ExpensesIntent

    data class SelectDate(
        val date: LocalDate,
    ) : ExpensesIntent
}

sealed interface ExpensesEffect {
    data class ShowError(
        val reason: FinanceFailureReason,
    ) : ExpensesEffect
}
