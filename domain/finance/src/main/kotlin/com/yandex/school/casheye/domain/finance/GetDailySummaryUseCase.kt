package com.yandex.school.casheye.domain.finance

import java.time.LocalDate

class GetDailySummaryUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult = repository.getDailySummary(date, currencyCode, transactionKind)
}
