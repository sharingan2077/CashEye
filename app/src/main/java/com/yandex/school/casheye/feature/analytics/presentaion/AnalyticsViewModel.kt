package com.yandex.school.casheye.feature.analytics.presentaion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.feature.analytics.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.math.BigDecimal
import javax.inject.Inject


data class AnalyticsUiState(
    val total: BigDecimal = BigDecimal.ZERO,
    val currencyCode: String = "RUB",
    val filters: List<Filter> = emptyList(),
    val transactions: List<Transaction> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = repository.observeAnalytics()
        .map { analytics ->
            AnalyticsUiState(
                total = analytics.total,
                currencyCode = analytics.currencyCode,
                filters = analytics.filters,
                transactions = analytics.transactions
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AnalyticsUiState()
        )

}