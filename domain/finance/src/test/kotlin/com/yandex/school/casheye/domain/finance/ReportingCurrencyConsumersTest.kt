package com.yandex.school.casheye.domain.finance

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.core.model.Transaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ReportingCurrencyConsumersTest {
    private val financeRepository = EmptyFinanceRepository()
    private val reportingCurrencyRepository = FakeReportingCurrencyRepository(CurrencyCode.USD)

    @Test
    fun `analytics keeps reporting currency while empty native summaries stay empty`() =
        runTest {
            val date = LocalDate.of(2026, 7, 26)

            val daily =
                GetDailySummaryUseCase(financeRepository, reportingCurrencyRepository)
                    .invoke(date, TransactionKind.Expense)
                    .first() as FinanceLoadResult.Success
            val accounts =
                GetAccountsUseCase(financeRepository, reportingCurrencyRepository)
                    .invoke()
                    .first() as AccountsLoadResult.Success
            val analytics =
                GetAnalyticsUseCase(financeRepository, reportingCurrencyRepository)
                    .invoke(
                        AnalyticsQuery(
                            startDate = date,
                            endDate = date,
                            transactionKind = AnalyticsTransactionKind.Expense,
                            accountId = null,
                            categoryIds = emptySet(),
                        ),
                    ).first() as AnalyticsLoadResult.Success

            assertEquals(emptyList<MoneyAmount>(), daily.summary.nativeTotals)
            assertEquals(emptyList<MoneyAmount>(), accounts.summary.nativeTotals)
            assertEquals(CurrencyCode.USD, analytics.summary.currencyCode)
        }
}

private class FakeReportingCurrencyRepository(
    initial: CurrencyCode,
) : ReportingCurrencyRepository {
    private val currency = MutableStateFlow(initial)

    override fun observe(): Flow<CurrencyCode> = currency

    override suspend fun set(currency: CurrencyCode) {
        this.currency.value = currency
    }
}

private class EmptyFinanceRepository : FinanceRepository {
    override fun observeAccounts() = MutableStateFlow(emptyList<Account>())

    override fun observeTransactions(query: TransactionsQuery) = MutableStateFlow(emptyList<Transaction>())

    override suspend fun getAccounts() = FinanceDataLoadResult.Success(emptyList<Account>())

    override suspend fun getTransactions(query: TransactionsQuery) =
        FinanceDataLoadResult.Success(emptyList<Transaction>())
}
