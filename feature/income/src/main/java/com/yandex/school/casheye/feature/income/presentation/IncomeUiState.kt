package com.yandex.school.casheye.feature.income.presentation

import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import java.math.BigDecimal
import java.time.LocalDate

sealed interface IncomeUiState {
    val isRefreshing: Boolean
        get() = false

    data object Loading : IncomeUiState

    data class Empty(
        override val isRefreshing: Boolean = false,
    ) : IncomeUiState

    data class Content(
        val total: BigDecimal,
        val currencyCode: String,
        val transactions: List<Transaction>,
        override val isRefreshing: Boolean = false,
    ) : IncomeUiState

    data class Error(
        val reason: FinanceFailureReason,
    ) : IncomeUiState
}

sealed interface IncomeIntent {
    data object Retry : IncomeIntent

    data object Refresh : IncomeIntent

    data class SelectDate(
        val date: LocalDate,
    ) : IncomeIntent

    data class DeleteTransaction(
        val id: Int,
    ) : IncomeIntent
}

sealed interface IncomeEffect {
    data class ShowError(
        val reason: FinanceFailureReason,
    ) : IncomeEffect

    data class ShowDeleteError(
        val reason: FinanceFailureReason,
    ) : IncomeEffect

    data object TransactionDeleted : IncomeEffect
}
