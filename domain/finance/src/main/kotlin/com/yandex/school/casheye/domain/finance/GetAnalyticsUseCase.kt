package com.yandex.school.casheye.domain.finance

class GetAnalyticsUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(query: AnalyticsQuery): AnalyticsLoadResult = repository.getAnalytics(query)
}
