package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.MoneyAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

class GetDailySummaryUseCase(
    private val repository: FinanceRepository,
    private val reportingCurrencyRepository: ReportingCurrencyRepository = DefaultReportingCurrencyRepository,
) {
    operator fun invoke(
        date: LocalDate,
        transactionKind: TransactionKind,
    ): Flow<FinanceLoadResult> =
        combine(
            repository.observeAccounts(),
            repository.observeTransactions(
                TransactionsQuery(
                    accountIds = emptySet(),
                    startDate = date,
                    endDate = date,
                ),
            ),
            reportingCurrencyRepository.observe(),
        ) { accounts, transactions, reportingCurrency ->
            val accountIds = accounts.mapTo(mutableSetOf()) { it.id }
            val finance =
                transactions
                    .filter { transaction ->
                        transaction.account.id in accountIds &&
                            when (transactionKind) {
                                TransactionKind.Income -> transaction.category.isIncome
                                TransactionKind.Expense -> !transaction.category.isIncome
                            }
                    }.sortedByDescending { it.transactionDate }

            val result: FinanceLoadResult =
                FinanceLoadResult.Success(
                    FinanceSummary(
                        nativeTotals =
                            aggregateNativeMoney(
                                amounts = finance.map { MoneyAmount(it.amount, it.currency) },
                                reportingCurrency = reportingCurrency,
                            ),
                        transactions = finance,
                    ),
                )
            result
        }.catch { emit(FinanceLoadResult.Failure(FinanceFailureReason.Unknown)) }

    suspend fun refresh(date: LocalDate): FinanceRefreshResult = repository.refreshPeriod(date, date)
}
