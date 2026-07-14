package com.yandex.school.casheye.domain.analytics.model

import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal

data class Analytics(
    val total: BigDecimal,
    val currencyCode: String,
    val filters: List<AnalyticsFilter>,
    val transactions: List<Transaction>
)
