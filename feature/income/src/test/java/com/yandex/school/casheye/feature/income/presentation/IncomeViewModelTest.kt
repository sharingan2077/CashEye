package com.yandex.school.casheye.feature.income.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsQuery
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.FinanceSummary
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.TransactionKind
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.ArrayDeque

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-07-17T14:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful income load exposes content`() =
        runTest {
            val summary = FinanceSummary(BigDecimal("125000.00"), "RUB", listOf(incomeTransaction()))
            val repository = FakeIncomeFinanceRepository(FinanceLoadResult.Success(summary))
            val viewModel = IncomeViewModel(GetDailySummaryUseCase(repository), clock)

            advanceUntilIdle()

            assertEquals(
                IncomeUiState.Content(summary.total, summary.currencyCode, summary.transactions),
                viewModel.state.value,
            )
            assertEquals(listOf(TransactionKind.Income), repository.requestedKinds)
        }

    @Test
    fun `empty income load exposes empty state`() =
        runTest {
            val viewModel =
                IncomeViewModel(
                    GetDailySummaryUseCase(
                        FakeIncomeFinanceRepository(
                            FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                        ),
                    ),
                    clock,
                )

            advanceUntilIdle()

            assertEquals(IncomeUiState.Empty, viewModel.state.value)
        }

    @Test
    fun `failure exposes error and emits show error effect`() =
        runTest {
            val viewModel =
                IncomeViewModel(
                    GetDailySummaryUseCase(
                        FakeIncomeFinanceRepository(FinanceLoadResult.Failure(FinanceFailureReason.Network)),
                    ),
                    clock,
                )
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

            advanceUntilIdle()

            assertTrue(viewModel.state.value is IncomeUiState.Error)
            assertEquals(IncomeEffect.ShowError("Проверьте подключение к интернету"), effect.await())
        }

    @Test
    fun `select date and retry load the selected date as income`() =
        runTest {
            val repository =
                FakeIncomeFinanceRepository(
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = IncomeViewModel(GetDailySummaryUseCase(repository), clock)
            val selectedDate = LocalDate.of(2026, 6, 18)

            advanceUntilIdle()
            viewModel.onIntent(IncomeIntent.SelectDate(selectedDate))
            advanceUntilIdle()
            viewModel.onIntent(IncomeIntent.Retry)
            advanceUntilIdle()

            assertEquals(
                listOf(LocalDate.of(2026, 7, 17), selectedDate, selectedDate),
                repository.requestedDates,
            )
            assertEquals(
                listOf(TransactionKind.Income, TransactionKind.Income, TransactionKind.Income),
                repository.requestedKinds,
            )
        }
}

private class FakeIncomeFinanceRepository(
    vararg results: FinanceLoadResult,
) : FinanceRepository {
    private val results = ArrayDeque(results.toList())
    val requestedDates = mutableListOf<LocalDate>()
    val requestedKinds = mutableListOf<TransactionKind>()

    override suspend fun getDailySummary(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult {
        requestedDates += date
        requestedKinds += transactionKind
        return results.removeFirst()
    }

    override suspend fun getAccountsSummary(currencyCode: String): AccountsLoadResult {
        TODO("Not yet implemented")
    }

    override suspend fun getAnalytics(query: AnalyticsQuery): AnalyticsLoadResult {
        TODO("Not yet implemented")
    }
}

private fun incomeTransaction(): Transaction {
    val instant = Instant.parse("2026-07-17T10:00:00Z")
    return Transaction(
        id = 1,
        account = Account(1, "Основной счёт", "💳", BigDecimal("1000.00"), "RUB"),
        category = Category(1, "Зарплата", "💰", true),
        amount = BigDecimal("125000.00"),
        transactionDate = instant,
        comment = null,
        createdAt = instant,
        updatedAt = instant,
    )
}
