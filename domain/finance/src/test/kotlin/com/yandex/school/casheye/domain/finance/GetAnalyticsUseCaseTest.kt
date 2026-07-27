package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

class GetAnalyticsUseCaseTest {
    private val transactionDate = LocalDate.of(2026, 7, 1)
    private val query =
        AnalyticsQuery(
            startDate = transactionDate,
            endDate = LocalDate.of(2026, 7, 31),
            transactionKind = AnalyticsTransactionKind.All,
            accountId = null,
            categoryIds = emptySet(),
        )

    @Test
    fun `historical USD amount does not change when a newer rate appears`() =
        runTest {
            val finance = AnalyticsFinanceRepository(listOf(transaction(CurrencyCode.USD, "100", isIncome = true)))
            val reporting = AnalyticsReportingCurrencyRepository(CurrencyCode.RUB)
            val rates =
                AnalyticsExchangeRateRepository(
                    rates =
                        listOf(
                            rate(CurrencyCode.USD, "1", transactionDate),
                            rate(CurrencyCode.RUB, "90", transactionDate),
                        ),
                )
            val useCase = useCase(finance, reporting, rates)

            val initial = useCase(query).first().successSummary()
            assertEquals(
                BigDecimal("9000"),
                initial.transactions
                    .single()
                    .reportingAmount.amount,
            )
            assertEquals(transactionDate, initial.transactions.single().rateDate)

            rates.emit(
                listOf(
                    rate(CurrencyCode.USD, "1", transactionDate),
                    rate(CurrencyCode.RUB, "90", transactionDate),
                    rate(CurrencyCode.USD, "1", LocalDate.of(2026, 7, 26)),
                    rate(CurrencyCode.RUB, "100", LocalDate.of(2026, 7, 26)),
                ),
            )

            val afterLatest = useCase(query).first().successSummary()
            assertEquals(
                BigDecimal("9000"),
                afterLatest.transactions
                    .single()
                    .reportingAmount.amount,
            )

            val accounts =
                GetAccountsUseCase(finance, reporting, rates)
                    .invoke()
                    .first() as AccountsLoadResult.Success
            assertEquals(
                BigDecimal("10000"),
                accounts.summary.currentValuation
                    ?.includedTotal
                    ?.amount,
            )

            reporting.set(CurrencyCode.USD)
            val inUsd = useCase(query).first().successSummary()
            assertEquals(
                BigDecimal("100"),
                inUsd.transactions
                    .single()
                    .reportingAmount.amount,
            )
        }

    @Test
    fun `weekend and cross currency conversion use previous cached rates`() =
        runTest {
            val friday = LocalDate.of(2026, 7, 3)
            val saturday = friday.plusDays(1)
            val finance =
                AnalyticsFinanceRepository(
                    listOf(transaction(CurrencyCode.CNY, "10", date = saturday)),
                )
            val rates =
                AnalyticsExchangeRateRepository(
                    rates =
                        listOf(
                            rate(CurrencyCode.CNY, "8", friday),
                            rate(CurrencyCode.GBP, "0.8", friday),
                        ),
                )

            val summary =
                useCase(
                    finance,
                    AnalyticsReportingCurrencyRepository(CurrencyCode.GBP),
                    rates,
                ).invoke(query.copy(startDate = saturday, endDate = saturday))
                    .first()
                    .successSummary()

            assertEquals(
                BigDecimal("1.0"),
                summary.transactions
                    .single()
                    .reportingAmount.amount,
            )
            assertEquals(friday, summary.transactions.single().rateDate)
        }

    @Test
    fun `missing rate is exposed instead of becoming a partial total`() =
        runTest {
            val finance = AnalyticsFinanceRepository(listOf(transaction(CurrencyCode.USD, "100")))
            val rates =
                AnalyticsExchangeRateRepository(
                    rates = listOf(rate(CurrencyCode.RUB, "90", transactionDate)),
                )

            val summary =
                useCase(
                    finance,
                    AnalyticsReportingCurrencyRepository(CurrencyCode.RUB),
                    rates,
                ).invoke(query)
                    .first()
                    .successSummary()

            assertEquals(BigDecimal.ZERO, summary.total)
            assertTrue(summary.transactions.isEmpty())
            assertEquals(
                CurrencyCode.USD,
                summary.unconvertedTransactions
                    .single()
                    .originalAmount.currency,
            )
            assertEquals(
                setOf(CurrencyCode.USD),
                summary.unconvertedTransactions.single().missingCurrencies,
            )
        }

    @Test
    fun `filters are applied before converted totals are built`() =
        runTest {
            val included = transaction(CurrencyCode.USD, "10", categoryId = 1, isIncome = true)
            val excluded = transaction(CurrencyCode.USD, "50", categoryId = 2)
            val finance = AnalyticsFinanceRepository(listOf(included, excluded))
            val rates =
                AnalyticsExchangeRateRepository(
                    rates =
                        listOf(
                            rate(CurrencyCode.USD, "1", transactionDate),
                            rate(CurrencyCode.RUB, "90", transactionDate),
                        ),
                )

            val summary =
                useCase(
                    finance,
                    AnalyticsReportingCurrencyRepository(CurrencyCode.RUB),
                    rates,
                ).invoke(
                    query.copy(
                        transactionKind = AnalyticsTransactionKind.Income,
                        categoryIds = setOf(1),
                    ),
                ).first()
                    .successSummary()

            assertEquals(listOf(included.id), summary.transactions.map { it.id })
            assertEquals(BigDecimal("900"), summary.total)
        }

    @Test
    fun `all total is converted income minus converted expenses`() =
        runTest {
            val finance =
                AnalyticsFinanceRepository(
                    listOf(
                        transaction(CurrencyCode.USD, "10", categoryId = 1, isIncome = true),
                        transaction(CurrencyCode.USD, "4", categoryId = 2),
                    ),
                )
            val rates =
                AnalyticsExchangeRateRepository(
                    rates =
                        listOf(
                            rate(CurrencyCode.USD, "1", transactionDate),
                            rate(CurrencyCode.RUB, "90", transactionDate),
                        ),
                )

            val summary =
                useCase(
                    finance,
                    AnalyticsReportingCurrencyRepository(CurrencyCode.RUB),
                    rates,
                ).invoke(query)
                    .first()
                    .successSummary()

            assertEquals(BigDecimal("540"), summary.total)
        }

    @Test
    fun `refresh pulls finance and ensures historical rates`() =
        runTest {
            val finance = AnalyticsFinanceRepository(emptyList())
            val rates = AnalyticsExchangeRateRepository(emptyList())

            val result =
                useCase(
                    finance,
                    AnalyticsReportingCurrencyRepository(CurrencyCode.RUB),
                    rates,
                ).refresh(query)

            assertEquals(FinanceRefreshResult.Success, result)
            assertEquals(query.startDate to query.endDate, finance.refreshedRange)
            assertEquals(query.startDate to query.endDate, rates.refreshedRange)
        }

    private fun useCase(
        finance: FinanceRepository,
        reporting: ReportingCurrencyRepository,
        rates: ExchangeRateRepository,
    ): GetAnalyticsUseCase =
        GetAnalyticsUseCase(
            repository = finance,
            reportingCurrencyRepository = reporting,
            exchangeRateRepository = rates,
            zoneId = ZoneOffset.UTC,
        )

    private fun transaction(
        currency: CurrencyCode,
        amount: String,
        date: LocalDate = transactionDate,
        categoryId: Int = 1,
        isIncome: Boolean = false,
    ): Transaction {
        val instant = date.atStartOfDay(ZoneOffset.UTC).toInstant()
        return Transaction(
            id = categoryId,
            account = account(currency),
            category = Category(categoryId, "Category $categoryId", "💱", isIncome),
            amount = BigDecimal(amount),
            currency = currency,
            transactionDate = instant,
            comment = null,
            createdAt = instant,
            updatedAt = instant,
        )
    }

    private fun account(currency: CurrencyCode): Account =
        Account(
            id = currency.ordinal + 1,
            name = currency.isoCode,
            emoji = "💳",
            balance = BigDecimal("100"),
            currency = currency,
        )

    private fun rate(
        currency: CurrencyCode,
        value: String,
        date: LocalDate,
    ): ExchangeRate =
        ExchangeRate(
            baseCurrency = CurrencyCode.EUR,
            quoteCurrency = currency,
            rate = BigDecimal(value),
            date = date,
        )
}

private fun AnalyticsLoadResult.successSummary(): AnalyticsSummary = (this as AnalyticsLoadResult.Success).summary

private class AnalyticsFinanceRepository(
    transactions: List<Transaction>,
) : FinanceRepository {
    private val accounts = MutableStateFlow(transactions.map { it.account }.distinctBy { it.id })
    private val transactions = MutableStateFlow(transactions)
    var refreshedRange: Pair<LocalDate, LocalDate>? = null

    override fun observeAccounts(): Flow<List<Account>> = accounts

    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> = transactions

    override suspend fun getAccounts(): FinanceDataLoadResult<List<Account>> =
        FinanceDataLoadResult.Success(accounts.value)

    override suspend fun getTransactions(query: TransactionsQuery): FinanceDataLoadResult<List<Transaction>> =
        FinanceDataLoadResult.Success(transactions.value)

    override suspend fun refreshPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
    ): FinanceRefreshResult {
        refreshedRange = startDate to endDate
        return FinanceRefreshResult.Success
    }
}

private class AnalyticsReportingCurrencyRepository(
    initial: CurrencyCode,
) : ReportingCurrencyRepository {
    private val currency = MutableStateFlow(initial)

    override fun observe(): Flow<CurrencyCode> = currency

    override suspend fun set(currency: CurrencyCode) {
        this.currency.value = currency
    }
}

private class AnalyticsExchangeRateRepository(
    rates: List<ExchangeRate>,
) : ExchangeRateRepository {
    private val snapshot =
        MutableStateFlow(
            ExchangeRateSnapshot(
                rates = rates,
                requestedFrom = null,
                requestedTo = null,
                missingCurrencies = emptySet(),
            ),
        )
    var refreshedRange: Pair<LocalDate, LocalDate>? = null

    override fun observeLatest(): Flow<ExchangeRateSnapshot> = snapshot

    override fun observeRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<ExchangeRateSnapshot> = snapshot

    override suspend fun refreshLatest(force: Boolean): ExchangeRateRefreshResult = ExchangeRateRefreshResult.Fresh

    override suspend fun refreshRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): ExchangeRateRefreshResult {
        refreshedRange = startDate to endDate
        return ExchangeRateRefreshResult.Fresh
    }

    fun emit(rates: List<ExchangeRate>) {
        snapshot.value = snapshot.value.copy(rates = rates)
    }
}
