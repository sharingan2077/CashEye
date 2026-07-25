package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import java.math.BigDecimal
import java.time.LocalDate

sealed interface ExpensesUiState {
    val isRefreshing: Boolean
        get() = false

    data object Loading : ExpensesUiState

    data class Empty(
        override val isRefreshing: Boolean = false,
    ) : ExpensesUiState

    data class Content(
        val total: BigDecimal,
        val currencyCode: String,
        val transactions: List<Transaction>,
        override val isRefreshing: Boolean = false,
    ) : ExpensesUiState

    data class Error(
        val reason: FinanceFailureReason,
    ) : ExpensesUiState
}

sealed interface ExpensesIntent {
    data object Retry : ExpensesIntent

    data object Refresh : ExpensesIntent

    data class SelectDate(
        val date: LocalDate,
    ) : ExpensesIntent

    data class DeleteTransaction(
        val id: Int,
    ) : ExpensesIntent
}

sealed interface ExpensesEffect {
    data class ShowError(
        val reason: FinanceFailureReason,
    ) : ExpensesEffect

    data class ShowDeleteError(
        val reason: FinanceFailureReason,
    ) : ExpensesEffect

    data object TransactionDeleted : ExpensesEffect
}
