package com.yandex.school.casheye.feature.expenses.presentation

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
class ExpensesViewModelTest {
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
    fun `successful load exposes content for expenses`() =
        runTest {
            val summary = FinanceSummary(BigDecimal("25.00"), "RUB", listOf(transaction()))
            val repository = FakeFinanceRepository(FinanceLoadResult.Success(summary))
            val viewModel = ExpensesViewModel(GetDailySummaryUseCase(repository), clock)

            advanceUntilIdle()

            assertEquals(
                ExpensesUiState.Content(summary.total, summary.currencyCode, summary.transactions),
                viewModel.state.value,
            )
            assertEquals(listOf(TransactionKind.Expense), repository.requestedKinds)
        }

    @Test
    fun `empty load exposes empty state`() =
        runTest {
            val viewModel =
                ExpensesViewModel(
                    GetDailySummaryUseCase(
                        FakeFinanceRepository(
                            FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                        ),
                    ),
                    clock,
                )

            advanceUntilIdle()

            assertEquals(ExpensesUiState.Empty, viewModel.state.value)
        }

    @Test
    fun `failure exposes error and emits show error effect`() =
        runTest {
            val viewModel =
                ExpensesViewModel(
                    GetDailySummaryUseCase(
                        FakeFinanceRepository(FinanceLoadResult.Failure(FinanceFailureReason.Network)),
                    ),
                    clock,
                )
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

            advanceUntilIdle()

            assertTrue(viewModel.state.value is ExpensesUiState.Error)
            assertEquals(ExpensesEffect.ShowError(FinanceFailureReason.Network), effect.await())
        }

    @Test
    fun `selecting date reloads the selected day`() =
        runTest {
            val repository =
                FakeFinanceRepository(
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = ExpensesViewModel(GetDailySummaryUseCase(repository), clock)
            val selectedDate = LocalDate.of(2026, 6, 18)

            advanceUntilIdle()
            viewModel.onIntent(ExpensesIntent.SelectDate(selectedDate))
            advanceUntilIdle()

            assertEquals(listOf(LocalDate.of(2026, 7, 17), selectedDate), repository.requestedDates)
        }
}

private class FakeFinanceRepository(
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

private fun transaction(): Transaction {
    val instant = Instant.parse("2026-07-17T10:00:00Z")
    return Transaction(
        id = 1,
        account = Account(1, "Основной счёт", "💵", BigDecimal("1000.00"), "RUB"),
        category = Category(1, "Продукты", "🛒", false),
        amount = BigDecimal("25.00"),
        transactionDate = instant,
        comment = null,
        createdAt = instant,
        updatedAt = instant,
    )
}
