package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Category
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
            val filtered = filterTransactions(transactions, query)
            val conversion = convertTransactions(filtered.transactions, reportingCurrency, rateSnapshot.rates)
            val result: AnalyticsLoadResult =
                AnalyticsLoadResult.Success(
                    AnalyticsSummary(
                        total = conversion.total(query.transactionKind),
                        currencyCode = reportingCurrency,
                        transactions = conversion.converted,
                        unconvertedTransactions = conversion.unconverted,
                        accounts = accounts,
                        availableCategories = filtered.availableCategories,
                    ),
                )
            result
        }.catch { emit(AnalyticsLoadResult.Failure(FinanceFailureReason.Unknown)) }

    private fun filterTransactions(
        transactions: List<Transaction>,
        query: AnalyticsQuery,
    ): FilteredAnalyticsTransactions {
        val kindFiltered = transactions.filter { it.matches(query.transactionKind) }
        return FilteredAnalyticsTransactions(
            availableCategories = kindFiltered.map { it.category }.distinctBy { it.id }.sortedBy { it.name },
            transactions =
                kindFiltered
                    .filter { query.categoryIds.isEmpty() || it.category.id in query.categoryIds }
                    .sortedByDescending { it.transactionDate },
        )
    }

    private fun convertTransactions(
        transactions: List<Transaction>,
        reportingCurrency: CurrencyCode,
        rates: Collection<ExchangeRate>,
    ): ConvertedAnalyticsTransactions {
        val converted = mutableListOf<AnalyticsTransaction>()
        val unconverted = mutableListOf<UnconvertedAnalyticsTransaction>()
        transactions.forEach { transaction ->
            val originalAmount = MoneyAmount(transaction.amount, transaction.currency)
            when (
                val result =
                    currencyConverter.convert(
                        originalAmount,
                        reportingCurrency,
                        transaction.localDate(),
                        rates,
                    )
            ) {
                is CurrencyConversionResult.Complete -> {
                    converted += transaction.toAnalyticsTransaction(originalAmount, result)
                }

                is CurrencyConversionResult.Incomplete -> {
                    unconverted +=
                        UnconvertedAnalyticsTransaction(transaction, originalAmount, result.missingCurrencies)
                }
            }
        }
        return ConvertedAnalyticsTransactions(converted, unconverted)
    }

    private fun Transaction.matches(kind: AnalyticsTransactionKind): Boolean =
        when (kind) {
            AnalyticsTransactionKind.Income -> category.isIncome
            AnalyticsTransactionKind.Expense -> !category.isIncome
            AnalyticsTransactionKind.All -> true
        }

    private fun Transaction.localDate() = transactionDate.atZone(zoneId).toLocalDate()

    private fun Transaction.toAnalyticsTransaction(
        originalAmount: MoneyAmount,
        result: CurrencyConversionResult.Complete,
    ) = AnalyticsTransaction(
        transaction = this,
        originalAmount = originalAmount,
        reportingAmount = result.money,
        rateDate = listOfNotNull(result.sourceRateDate, result.targetRateDate).minOrNull(),
    )

    private data class FilteredAnalyticsTransactions(
        val availableCategories: List<Category>,
        val transactions: List<Transaction>,
    )

    private data class ConvertedAnalyticsTransactions(
        val converted: List<AnalyticsTransaction>,
        val unconverted: List<UnconvertedAnalyticsTransaction>,
    ) {
        fun total(kind: AnalyticsTransactionKind): BigDecimal =
            converted.fold(BigDecimal.ZERO) { total, transaction ->
                val amount = transaction.reportingAmount.amount.abs()
                if (kind == AnalyticsTransactionKind.All && !transaction.transaction.category.isIncome) {
                    total - amount
                } else {
                    total + amount
                }
            }
    }

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
