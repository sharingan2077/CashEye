package com.yandex.school.casheye.feature.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.feature.expenses.domain.repository.ExpensesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal

data class ExpensesUiState(
    val total: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String = "RUB",
    val transactions: List<Transaction> = emptyList(),
)

@HiltViewModel
class ExpensesViewModel @Inject constructor(
    private val repository: ExpensesRepository
) : ViewModel() {

    val uiState: StateFlow<ExpensesUiState> = repository.observeExpenses()
        .map { expenses ->
            ExpensesUiState(
                total = expenses.total,
                currencyCode = expenses.currencyCode,
                transactions = expenses.transactions
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = ExpensesUiState()
        )

}