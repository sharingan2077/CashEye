package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.domain.finance.currency.CurrencyConversionResult
import com.yandex.school.casheye.domain.finance.currency.CurrencyConverter
import com.yandex.school.casheye.domain.finance.currency.DefaultReportingCurrencyRepository
import com.yandex.school.casheye.domain.finance.currency.EmptyExchangeRateRepository
import com.yandex.school.casheye.domain.finance.currency.ExchangeRateRefreshResult
import com.yandex.school.casheye.domain.finance.currency.ExchangeRateRepository
import com.yandex.school.casheye.domain.finance.currency.ReportingCurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.ZoneId

class GetAnalyticsUseCase(
    private val repository: FinanceQueryRepository,
    private val reportingCurrencyRepository: ReportingCurrencyRepository = DefaultReportingCurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository = EmptyExchangeRateRepository,
    private val currencyConverter: CurrencyConverter = CurrencyConverter(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
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
            reportingCurrencyRepository.observe(),
            exchangeRateRepository.observeRange(query.startDate, query.endDate),
        ) { accounts, transactions, reportingCurrency, rateSnapshot ->
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
            val convertedTransactions = mutableListOf<AnalyticsTransaction>()
            val unconvertedTransactions = mutableListOf<UnconvertedAnalyticsTransaction>()
            filtered.forEach { transaction ->
                val originalAmount = MoneyAmount(transaction.amount, transaction.currency)
                val transactionDate = transaction.transactionDate.atZone(zoneId).toLocalDate()
                when (
                    val conversion =
                        currencyConverter.convert(
                            money = originalAmount,
                            target = reportingCurrency,
                            date = transactionDate,
                            rates = rateSnapshot.rates,
                        )
                ) {
                    is CurrencyConversionResult.Complete -> {
                        convertedTransactions +=
                            AnalyticsTransaction(
                                transaction = transaction,
                                originalAmount = originalAmount,
                                reportingAmount = conversion.money,
                                rateDate =
                                    listOfNotNull(conversion.sourceRateDate, conversion.targetRateDate)
                                        .minOrNull(),
                            )
                    }

                    is CurrencyConversionResult.Incomplete -> {
                        unconvertedTransactions +=
                            UnconvertedAnalyticsTransaction(
                                transaction = transaction,
                                originalAmount = originalAmount,
                                missingCurrencies = conversion.missingCurrencies,
                            )
                    }
                }
            }

            val result: AnalyticsLoadResult =
                AnalyticsLoadResult.Success(
                    AnalyticsSummary(
                        total =
                            convertedTransactions.fold(BigDecimal.ZERO) { total, transaction ->
                                val amount = transaction.reportingAmount.amount.abs()
                                when (query.transactionKind) {
                                    AnalyticsTransactionKind.All -> {
                                        if (transaction.category.isIncome) total + amount else total - amount
                                    }

                                    AnalyticsTransactionKind.Income,
                                    AnalyticsTransactionKind.Expense,
                                    -> {
                                        total + amount
                                    }
                                }
                            },
                        currencyCode = reportingCurrency,
                        transactions = convertedTransactions,
                        unconvertedTransactions = unconvertedTransactions,
                        accounts = accounts,
                        availableCategories = availableCategories,
                    ),
                )
            result
        }.catch { emit(AnalyticsLoadResult.Failure(FinanceFailureReason.Unknown)) }

    suspend fun refresh(query: AnalyticsQuery): FinanceRefreshResult {
        val financeResult = repository.refreshPeriod(query.startDate, query.endDate)
        val rateResult = exchangeRateRepository.refreshRange(query.startDate, query.endDate)
        if (financeResult is FinanceRefreshResult.Failure) return financeResult
        return rateResult.toFinanceRefreshResult()
    }

    private fun ExchangeRateRefreshResult.toFinanceRefreshResult(): FinanceRefreshResult =
        when (this) {
            ExchangeRateRefreshResult.Fresh,
            ExchangeRateRefreshResult.Updated,
            is ExchangeRateRefreshResult.Incomplete,
            -> {
                FinanceRefreshResult.Success
            }

            is ExchangeRateRefreshResult.TemporaryFailure -> {
                if (cachedDataAvailable) {
                    FinanceRefreshResult.Success
                } else {
                    FinanceRefreshResult.Failure(FinanceFailureReason.Network, hasUsableCache = false)
                }
            }

            is ExchangeRateRefreshResult.PermanentFailure -> {
                if (cachedDataAvailable) {
                    FinanceRefreshResult.Success
                } else {
                    FinanceRefreshResult.Failure(FinanceFailureReason.Unknown, hasUsableCache = false)
                }
            }
        }
}
