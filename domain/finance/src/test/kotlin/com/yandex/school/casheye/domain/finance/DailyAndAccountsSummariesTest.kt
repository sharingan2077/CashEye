package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate

class DailyAndAccountsSummariesTest {
    private val date = LocalDate.of(2026, 7, 26)

    @Test
    fun `daily summary groups snapshot currencies and puts reporting currency first`() =
        runTest {
            val rubAccount = account(1, CurrencyCode.RUB, "0")
            val changedAccount = account(2, CurrencyCode.CNY, "0")
            val repository =
                FakeFinanceRepository(
                    accounts = listOf(rubAccount, changedAccount),
                    transactions =
                        listOf(
                            transaction(1, rubAccount, CurrencyCode.RUB, "12000"),
                            transaction(2, changedAccount, CurrencyCode.USD, "100"),
                            transaction(3, changedAccount, CurrencyCode.USD, "50"),
                        ),
                )

            val result =
                GetDailySummaryUseCase(
                    repository = repository,
                    reportingCurrencyRepository = SummaryReportingCurrencyRepository(CurrencyCode.USD),
                ).invoke(date, TransactionKind.Expense)
                    .first() as FinanceLoadResult.Success

            assertEquals(
                listOf(
                    MoneyAmount(BigDecimal("150"), CurrencyCode.USD),
                    MoneyAmount(BigDecimal("12000"), CurrencyCode.RUB),
                ),
                result.summary.nativeTotals,
            )
        }

    @Test
    fun `accounts valuation uses latest cross rates`() =
        runTest {
            val rateDate = LocalDate.of(2026, 7, 25)
            val repository =
                FakeFinanceRepository(
                    accounts =
                        listOf(
                            account(1, CurrencyCode.RUB, "1000"),
                            account(2, CurrencyCode.USD, "100"),
                        ),
                )
            val useCase =
                GetAccountsUseCase(
                    repository = repository,
                    reportingCurrencyRepository = SummaryReportingCurrencyRepository(CurrencyCode.RUB),
                    exchangeRateRepository =
                        FakeExchangeRateRepository(
                            listOf(
                                rate(CurrencyCode.RUB, "80", rateDate),
                                rate(CurrencyCode.USD, "0.8", rateDate),
                            ),
                        ),
                )

            val summary = (useCase().first() as AccountsLoadResult.Success).summary

            assertEquals(
                listOf(
                    MoneyAmount(BigDecimal("1000"), CurrencyCode.RUB),
                    MoneyAmount(BigDecimal("100"), CurrencyCode.USD),
                ),
                summary.nativeTotals,
            )
            assertEquals(
                0,
                requireNotNull(summary.currentValuation?.includedTotal)
                    .amount
                    .compareTo(BigDecimal("11000")),
            )
            assertEquals(CurrencyCode.RUB, summary.currentValuation.includedTotal.currency)
            assertEquals(rateDate, summary.currentValuation.rateDate)
            assertEquals(true, summary.currentValuation.isComplete)
        }

    @Test
    fun `reporting currency change reorders native totals and recalculates valuation only`() =
        runTest {
            val rateDate = LocalDate.of(2026, 7, 25)
            val accounts =
                listOf(
                    account(1, CurrencyCode.RUB, "1000"),
                    account(2, CurrencyCode.USD, "100"),
                )
            val reportingCurrency = SummaryReportingCurrencyRepository(CurrencyCode.RUB)
            val useCase =
                GetAccountsUseCase(
                    repository = FakeFinanceRepository(accounts),
                    reportingCurrencyRepository = reportingCurrency,
                    exchangeRateRepository =
                        FakeExchangeRateRepository(
                            listOf(
                                rate(CurrencyCode.RUB, "80", rateDate),
                                rate(CurrencyCode.USD, "0.8", rateDate),
                            ),
                        ),
                )

            reportingCurrency.set(CurrencyCode.USD)
            val summary = (useCase().first() as AccountsLoadResult.Success).summary

            assertEquals(CurrencyCode.USD, summary.nativeTotals.first().currency)
            assertEquals(accounts, summary.accounts)
            assertEquals(CurrencyCode.USD, summary.currentValuation?.includedTotal?.currency)
            assertEquals(
                0,
                requireNotNull(summary.currentValuation?.includedTotal)
                    .amount
                    .compareTo(BigDecimal("110")),
            )
        }

    @Test
    fun `zero native groups are omitted`() {
        assertEquals(
            emptyList<MoneyAmount>(),
            aggregateNativeMoney(
                amounts =
                    listOf(
                        MoneyAmount(BigDecimal.TEN, CurrencyCode.GBP),
                        MoneyAmount(BigDecimal.TEN.negate(), CurrencyCode.GBP),
                    ),
                reportingCurrency = CurrencyCode.RUB,
            ),
        )
    }

    @Test
    fun `missing rate exposes excluded native total instead of a complete zero`() =
        runTest {
            val repository =
                FakeFinanceRepository(
                    accounts =
                        listOf(
                            account(1, CurrencyCode.RUB, "1000"),
                            account(2, CurrencyCode.CNY, "50"),
                        ),
                )
            val summary =
                (
                    GetAccountsUseCase(
                        repository = repository,
                        reportingCurrencyRepository = SummaryReportingCurrencyRepository(CurrencyCode.RUB),
                    ).invoke()
                        .first() as AccountsLoadResult.Success
                ).summary

            assertEquals(
                MoneyAmount(BigDecimal("1000"), CurrencyCode.RUB),
                summary.currentValuation?.includedTotal,
            )
            assertEquals(
                listOf(MoneyAmount(BigDecimal("50"), CurrencyCode.CNY)),
                summary.currentValuation?.excludedNativeTotals,
            )
            assertFalse(requireNotNull(summary.currentValuation).isComplete)
            assertNull(summary.currentValuation.rateDate)
        }

    private fun account(
        id: Int,
        currency: CurrencyCode,
        balance: String,
    ) = Account(id, "Account $id", "💳", BigDecimal(balance), currency)

    private fun transaction(
        id: Int,
        account: Account,
        currency: CurrencyCode,
        amount: String,
    ) = Transaction(
        id = id,
        account = account,
        category = Category(id, "Category $id", "🧾", false),
        amount = BigDecimal(amount),
        currency = currency,
        transactionDate = Instant.parse("2026-07-26T12:00:00Z"),
        comment = null,
        createdAt = Instant.parse("2026-07-26T12:00:00Z"),
        updatedAt = Instant.parse("2026-07-26T12:00:00Z"),
    )

    private fun rate(
        quote: CurrencyCode,
        value: String,
        date: LocalDate,
    ) = ExchangeRate(CurrencyCode.EUR, quote, BigDecimal(value), date)
}

private class FakeFinanceRepository(
    accounts: List<Account>,
    transactions: List<Transaction> = emptyList(),
) : FinanceRepository {
    private val accountsFlow = MutableStateFlow(accounts)
    private val transactionsFlow = MutableStateFlow(transactions)

    override fun observeAccounts(): Flow<List<Account>> = accountsFlow

    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> = transactionsFlow

    override suspend fun getAccounts() = FinanceDataLoadResult.Success(accountsFlow.value)

    override suspend fun getTransactions(query: TransactionsQuery) =
        FinanceDataLoadResult.Success(transactionsFlow.value)
}

private class SummaryReportingCurrencyRepository(
    initial: CurrencyCode,
) : ReportingCurrencyRepository {
    private val currency = MutableStateFlow(initial)

    override fun observe(): Flow<CurrencyCode> = currency

    override suspend fun set(currency: CurrencyCode) {
        this.currency.value = currency
    }
}

private class FakeExchangeRateRepository(
    rates: List<ExchangeRate>,
) : ExchangeRateRepository {
    private val snapshot = MutableStateFlow(ExchangeRateSnapshot(rates, null, null, emptySet()))

    override fun observeLatest(): Flow<ExchangeRateSnapshot> = snapshot

    override fun observeRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<ExchangeRateSnapshot> = snapshot

    override suspend fun refreshLatest(force: Boolean) = ExchangeRateRefreshResult.Fresh

    override suspend fun refreshRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ) = ExchangeRateRefreshResult.Fresh
}
