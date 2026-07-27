package com.yandex.school.casheye.domain.finance.currency

import com.yandex.school.casheye.core.model.CurrencyCode
import kotlinx.coroutines.flow.Flow

class ObserveReportingCurrencyUseCase(
    private val repository: ReportingCurrencyRepository,
) {
    operator fun invoke(): Flow<CurrencyCode> = repository.observe()
}

class SetReportingCurrencyUseCase(
    private val repository: ReportingCurrencyRepository,
) {
    suspend operator fun invoke(currency: CurrencyCode) = repository.set(currency)
}
