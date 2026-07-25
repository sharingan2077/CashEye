package com.yandex.school.casheye.domain.finance

import java.math.BigDecimal

class GetAnalyticsUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(query: AnalyticsQuery): AnalyticsLoadResult {
        val accounts =
            when (val result = repository.getAccounts()) {
                is FinanceDataLoadResult.Success -> result.data
                is FinanceDataLoadResult.Failure -> return AnalyticsLoadResult.Failure(result.reason)
            }

        val requestedAccounts =
            query.accountId?.let { selectedId -> accounts.filter { it.id == selectedId } } ?: accounts
        val transactions =
            repository.getTransactions(
                TransactionsQuery(
                    accountIds = requestedAccounts.mapTo(mutableSetOf()) { it.id },
                    startDate = query.startDate,
                    endDate = query.endDate,
                ),
            )
        val transactionList =
            when (transactions) {
                is FinanceDataLoadResult.Success -> transactions.data
                is FinanceDataLoadResult.Failure -> return AnalyticsLoadResult.Failure(transactions.reason)
            }

        val kindFiltered =
            transactionList.filter { transaction ->
                when (query.transactionKind) {
                    AnalyticsTransactionKind.Income -> transaction.category.isIncome
                    AnalyticsTransactionKind.Expense -> !transaction.category.isIncome
                    AnalyticsTransactionKind.All -> true
                }
            }
        val availableCategories = kindFiltered.map { it.category }.distinctBy { it.id }.sortedBy { it.name }
        val filtered =
            kindFiltered
                .filter { transaction -> query.categoryIds.isEmpty() || transaction.category.id in query.categoryIds }
                .sortedByDescending { it.transactionDate }

        return AnalyticsLoadResult.Success(
            AnalyticsSummary(
                total = filtered.fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount },
                currencyCode = query.currencyCode,
                transactions = filtered,
                accounts = accounts,
                availableCategories = availableCategories,
            ),
        )
    }
}
