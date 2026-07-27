package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate

data class AnalyticsQuery(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val transactionKind: AnalyticsTransactionKind,
    val accountId: Int?,
    val categoryIds: Set<Int>,
)

enum class AnalyticsTransactionKind {
    Income,
    Expense,
    All,
}

data class AnalyticsSummary(
    val total: BigDecimal,
    val currencyCode: CurrencyCode,
    val transactions: List<AnalyticsTransaction>,
    val unconvertedTransactions: List<UnconvertedAnalyticsTransaction>,
    val accounts: List<Account>,
    val availableCategories: List<Category>,
)

data class AnalyticsTransaction(
    val transaction: Transaction,
    val originalAmount: MoneyAmount,
    val reportingAmount: MoneyAmount,
    val rateDate: LocalDate?,
) {
    val id: Int
        get() = transaction.id

    val category: Category
        get() = transaction.category

    val transactionDate
        get() = transaction.transactionDate
}

data class UnconvertedAnalyticsTransaction(
    val transaction: Transaction,
    val originalAmount: MoneyAmount,
    val missingCurrencies: Set<CurrencyCode>,
) {
    val id: Int
        get() = transaction.id
}

sealed interface AnalyticsLoadResult {
    data class Success(
        val summary: AnalyticsSummary,
    ) : AnalyticsLoadResult

    data class Failure(
        val reason: FinanceFailureReason,
    ) : AnalyticsLoadResult
}
