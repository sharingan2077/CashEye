package com.yandex.school.casheye.domain.finance

import java.math.BigDecimal
import java.time.LocalDate

class GetDailySummaryUseCase(
    private val repository: FinanceRepository,
) {
    suspend operator fun invoke(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult {
        val accounts =
            when (val result = repository.getAccounts()) {
                is FinanceDataLoadResult.Success -> result.data
                is FinanceDataLoadResult.Failure -> return FinanceLoadResult.Failure(result.reason)
            }

        val transactions =
            repository.getTransactions(
                TransactionsQuery(
                    accountIds = accounts.mapTo(mutableSetOf()) { it.id },
                    startDate = date,
                    endDate = date,
                ),
            )
        val transactionList =
            when (transactions) {
                is FinanceDataLoadResult.Success -> transactions.data
                is FinanceDataLoadResult.Failure -> return FinanceLoadResult.Failure(transactions.reason)
            }

        val finance =
            transactionList
                .filter { transaction ->
                    when (transactionKind) {
                        TransactionKind.Income -> transaction.category.isIncome
                        TransactionKind.Expense -> !transaction.category.isIncome
                    }
                }.sortedByDescending { it.transactionDate }

        return FinanceLoadResult.Success(
            FinanceSummary(
                total = finance.fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount },
                currencyCode = currencyCode,
                transactions = finance,
            ),
        )
    }
}
