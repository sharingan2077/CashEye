package com.yandex.school.casheye.feature.analytics.presentation

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal

data class AnalyticsUiState(
    val total: BigDecimal,
    val currencyCode: String,
    val filters: List<Filter>,
    val transactions: List<Transaction>,
)
