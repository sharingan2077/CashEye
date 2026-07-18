package com.yandex.school.casheye.feature.income.presentation

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate

sealed interface IncomeUiState {
    data object Loading : IncomeUiState

    data object Empty : IncomeUiState

    data class Content(
        val total: BigDecimal,
        val currencyCode: String,
        val transactions: List<Transaction>,
    ) : IncomeUiState

    data class Error(
        val message: String,
    ) : IncomeUiState
}

sealed interface IncomeIntent {
    data object Retry : IncomeIntent

    data class SelectDate(
        val date: LocalDate,
    ) : IncomeIntent
}

sealed interface IncomeEffect {
    data class ShowError(
        val message: String,
    ) : IncomeEffect
}
