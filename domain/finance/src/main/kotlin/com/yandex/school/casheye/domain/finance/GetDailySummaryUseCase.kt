package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.currency.CurrencyConversionResult
import com.yandex.school.casheye.domain.finance.currency.CurrencyConverter
import com.yandex.school.casheye.domain.finance.currency.DefaultReportingCurrencyRepository
import com.yandex.school.casheye.domain.finance.currency.EmptyExchangeRateRepository
import com.yandex.school.casheye.domain.finance.currency.ExchangeRate
import com.yandex.school.casheye.domain.finance.currency.ExchangeRateRefreshResult
import com.yandex.school.casheye.domain.finance.currency.ExchangeRateRepository
import com.yandex.school.casheye.domain.finance.currency.ReportingCurrencyRepository
import com.yandex.school.casheye.domain.finance.currency.aggregateNativeMoney
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId

class GetDailySummaryUseCase(
    private val repository: FinanceQueryRepository,
    private val reportingCurrencyRepository: ReportingCurrencyRepository = DefaultReportingCurrencyRepository,
    private val exchangeRateRepository: ExchangeRateRepository = EmptyExchangeRateRepository,
    private val currencyConverter: CurrencyConverter = CurrencyConverter(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {
    operator fun invoke(
        date: LocalDate,
        transactionKind: TransactionKind,
    ): Flow<FinanceLoadResult> = invoke(date, date, transactionKind)

    operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate,
        transactionKind: TransactionKind,
    ): Flow<FinanceLoadResult> =
        combine(
            repository.observeAccounts(),
            repository.observeTransactions(
                TransactionsQuery(
                    accountIds = emptySet(),
                    startDate = startDate,
                    endDate = endDate,
                ),
            ),
            reportingCurrencyRepository.observe(),
            exchangeRateRepository.observeRange(startDate, endDate),
        ) { accounts, transactions, reportingCurrency, rateSnapshot ->
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
                        currentValuation =
                            calculateCurrentValuation(
                                transactions = finance,
                                reportingCurrency = reportingCurrency,
                                rates = rateSnapshot.rates,
                            ),
                        transactions = finance,
                    ),
                )
            result
        }.catch { emit(FinanceLoadResult.Failure(FinanceFailureReason.Unknown)) }

    suspend fun refresh(date: LocalDate): FinanceRefreshResult = refresh(date, date)

    suspend fun refresh(
        startDate: LocalDate,
        endDate: LocalDate,
    ): FinanceRefreshResult {
        val financeResult = repository.refreshPeriod(startDate, endDate)
        val rateResult = exchangeRateRepository.refreshRange(startDate, endDate)
        if (financeResult is FinanceRefreshResult.Failure) return financeResult
        return rateResult.toFinanceRefreshResult()
    }

    private fun calculateCurrentValuation(
        transactions: List<Transaction>,
        reportingCurrency: CurrencyCode,
        rates: List<ExchangeRate>,
    ): DailyCurrentValuation? {
        if (transactions.isEmpty()) return null

        var included = BigDecimal.ZERO
        var hasIncludedAmount = false
        val excluded = mutableListOf<MoneyAmount>()

        transactions.forEach { transaction ->
            val originalAmount = MoneyAmount(transaction.amount, transaction.currency)
            when (
                val conversion =
                    currencyConverter.convert(
                        money = originalAmount,
                        target = reportingCurrency,
                        date = transaction.transactionDate.atZone(zoneId).toLocalDate(),
                        rates = rates,
                    )
            ) {
                is CurrencyConversionResult.Complete -> {
                    included += conversion.money.amount
                    hasIncludedAmount = true
                }

                is CurrencyConversionResult.Incomplete -> excluded += originalAmount
            }
        }

        return DailyCurrentValuation(
            includedTotal = included.takeIf { hasIncludedAmount }?.let { MoneyAmount(it, reportingCurrency) },
            excludedNativeTotals = aggregateNativeMoney(excluded, reportingCurrency),
        )
    }

    private fun ExchangeRateRefreshResult.toFinanceRefreshResult(): FinanceRefreshResult =
        when (this) {
            ExchangeRateRefreshResult.Fresh,
            ExchangeRateRefreshResult.Updated,
            is ExchangeRateRefreshResult.Incomplete,
            -> FinanceRefreshResult.Success

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
