package com.yandex.school.casheye.feature.transactions.income.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.transactions.repository.IncomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import javax.inject.Inject


data class IncomeUiState(
    val total: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String = "RUB",
    val transactions: List<Transaction> = emptyList(),
)

@HiltViewModel
class IncomeViewModel @Inject constructor(private val repository: IncomeRepository) : ViewModel() {


    val state: StateFlow<IncomeUiState> = repository.observeIncome()
        .map { income ->
            IncomeUiState(
                total = income.total,
                currencyCode = income.currencyCode,
                transactions = income.transactions
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000L),
            initialValue = IncomeUiState()
        )


}