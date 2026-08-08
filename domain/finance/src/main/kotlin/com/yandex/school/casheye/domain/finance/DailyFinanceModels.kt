package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.core.model.Transaction

enum class TransactionKind {
    Income,
    Expense,
}

data class FinanceSummary(
    val nativeTotals: List<MoneyAmount>,
    val transactions: List<Transaction>,
    val currentValuation: DailyCurrentValuation? = null,
)

data class DailyCurrentValuation(
    val includedTotal: MoneyAmount?,
    val excludedNativeTotals: List<MoneyAmount>,
) {
    val isComplete: Boolean
        get() = excludedNativeTotals.isEmpty()
}

sealed interface FinanceLoadResult {
    data class Success(
        val summary: FinanceSummary,
    ) : FinanceLoadResult

    data class Failure(
        val reason: FinanceFailureReason,
    ) : FinanceLoadResult
}
