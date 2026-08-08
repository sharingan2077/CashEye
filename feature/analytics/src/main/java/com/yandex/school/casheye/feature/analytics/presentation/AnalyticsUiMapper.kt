package com.yandex.school.casheye.feature.analytics.presentation

import com.yandex.school.casheye.domain.finance.AnalyticsSummary
import com.yandex.school.casheye.domain.finance.AnalyticsTransaction
import com.yandex.school.casheye.domain.finance.UnconvertedAnalyticsTransaction
import java.math.BigDecimal

internal object AnalyticsUiMapper {
    data class LoadedUiModels(
        val data: AnalyticsScreenData,
        val transactions: List<AnalyticsTransaction>,
        val unconvertedTransactions: List<UnconvertedAnalyticsTransaction>,
        val categorySummaries: List<AnalyticsCategorySummary>,
        val typeSummaries: List<AnalyticsTypeSummary>,
    )

    fun map(
        summary: AnalyticsSummary,
        screenData: AnalyticsScreenData,
    ): LoadedUiModels {
        val transactions = summary.transactions.sortedByDescending { it.transactionDate }
        return LoadedUiModels(
            data = screenData.copy(accounts = summary.accounts, categories = summary.availableCategories),
            transactions = transactions,
            unconvertedTransactions =
                summary.unconvertedTransactions.sortedByDescending {
                    it.transaction.transactionDate
                },
            categorySummaries = transactions.toCategorySummaries(),
            typeSummaries = transactions.toTypeSummaries(),
        )
    }
}

internal fun List<AnalyticsTransaction>.toCategorySummaries(): List<AnalyticsCategorySummary> =
    groupBy { it.category.id }
        .values
        .map { transactions ->
            AnalyticsCategorySummary(
                category = transactions.first().category,
                amount =
                    transactions.fold(BigDecimal.ZERO) { total, transaction ->
                        total +
                            transaction.reportingAmount.amount
                    },
            )
        }.sortedByDescending { it.amount }

internal fun List<AnalyticsTransaction>.toTypeSummaries(): List<AnalyticsTypeSummary> =
    listOf(
        AnalyticsType.Expenses to filterNot { it.category.isIncome },
        AnalyticsType.Income to filter { it.category.isIncome },
    ).mapNotNull { (type, transactions) ->
        val amount =
            transactions.fold(BigDecimal.ZERO) { total, transaction ->
                total +
                    transaction.reportingAmount.amount.abs()
            }
        amount.takeIf { it.signum() != 0 }?.let { AnalyticsTypeSummary(type, it) }
    }.sortedByDescending { it.amount.abs() }
