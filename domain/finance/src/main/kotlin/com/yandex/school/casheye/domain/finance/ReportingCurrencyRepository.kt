package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.CurrencyCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ReportingCurrencyRepository {
    fun observe(): Flow<CurrencyCode>

    suspend fun set(currency: CurrencyCode)
}

internal object DefaultReportingCurrencyRepository : ReportingCurrencyRepository {
    override fun observe(): Flow<CurrencyCode> = flowOf(DEFAULT_REPORTING_CURRENCY)

    override suspend fun set(currency: CurrencyCode) = Unit
}
