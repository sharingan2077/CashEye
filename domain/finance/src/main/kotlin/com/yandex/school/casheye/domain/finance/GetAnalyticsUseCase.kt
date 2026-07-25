package com.yandex.school.casheye.domain.finance

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal

class GetAnalyticsUseCase(
    private val repository: FinanceRepository,
) {
    operator fun invoke(query: AnalyticsQuery): Flow<AnalyticsLoadResult> =
        combine(
            repository.observeAccounts(),
            repository.observeTransactions(
                TransactionsQuery(
                    accountIds = query.accountId?.let(::setOf) ?: emptySet(),
                    startDate = query.startDate,
                    endDate = query.endDate,
                ),
            ),
        ) { accounts, transactions ->
            val kindFiltered =
                transactions.filter { transaction ->
                    when (query.transactionKind) {
                        AnalyticsTransactionKind.Income -> transaction.category.isIncome
                        AnalyticsTransactionKind.Expense -> !transaction.category.isIncome
                        AnalyticsTransactionKind.All -> true
                    }
                }
            val availableCategories = kindFiltered.map { it.category }.distinctBy { it.id }.sortedBy { it.name }
            val filtered =
                kindFiltered
                    .filter { transaction ->
                        query.categoryIds.isEmpty() || transaction.category.id in query.categoryIds
                    }.sortedByDescending { it.transactionDate }

            val result: AnalyticsLoadResult =
                AnalyticsLoadResult.Success(
                    AnalyticsSummary(
                        total = filtered.fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount },
                        currencyCode = query.currencyCode,
                        transactions = filtered,
                        accounts = accounts,
                        availableCategories = availableCategories,
                    ),
                )
            result
        }.catch { emit(AnalyticsLoadResult.Failure(FinanceFailureReason.Unknown)) }

    suspend fun refresh(query: AnalyticsQuery): FinanceRefreshResult =
        repository.refreshPeriod(query.startDate, query.endDate)
}
