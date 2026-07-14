package com.yandex.school.casheye.feature.analytics.domain.model

import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.feature.analytics.presentaion.Filter
import java.math.BigDecimal

data class Analytics(
    val total: BigDecimal,
    val currencyCode: String,
    val filters: List<Filter>,
    val transactions: List<Transaction>
)