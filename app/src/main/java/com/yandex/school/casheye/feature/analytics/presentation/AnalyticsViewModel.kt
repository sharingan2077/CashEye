package com.yandex.school.casheye.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.analytics.model.AnalyticsFilter
import com.yandex.school.casheye.domain.analytics.repository.AnalyticsRepository
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
    val filters: List<AnalyticsFilterUi> = emptyList(),
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
                filters = analytics.filters.map(AnalyticsFilter::toUi),
                transactions = analytics.transactions
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AnalyticsUiState()
        )

}

private fun AnalyticsFilter.toUi(): AnalyticsFilterUi = when (this) {
    is AnalyticsFilter.Type -> AnalyticsFilterUi.Type(type = type)
    is AnalyticsFilter.Period -> AnalyticsFilterUi.Period(period = period)
    is AnalyticsFilter.Articles -> AnalyticsFilterUi.Articles(articles = articles)
    is AnalyticsFilter.Account -> AnalyticsFilterUi.Account(accounts = accounts)
}
