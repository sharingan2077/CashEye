package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.FinanceDataLoadResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.FinanceSummary
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.TransactionsQuery
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

            assertEquals(ExpensesUiState.Empty(), viewModel.state.value)
        }

    @Test
    fun `initial failure exposes error state`() =
        runTest {
            val viewModel =
                ExpensesViewModel(
                    GetDailySummaryUseCase(
                        FakeFinanceRepository(FinanceLoadResult.Failure(FinanceFailureReason.Network)),
                    ),
                    clock,
                )

            advanceUntilIdle()

            assertTrue(viewModel.state.value is ExpensesUiState.Error)
        }

    @Test
    fun `failed refresh keeps content and emits show error effect`() =
        runTest {
            val summary = FinanceSummary(BigDecimal("25.00"), "RUB", listOf(transaction()))
            val viewModel =
                ExpensesViewModel(
                    GetDailySummaryUseCase(
                        FakeFinanceRepository(
                            FinanceLoadResult.Success(summary),
                            FinanceLoadResult.Failure(FinanceFailureReason.Network),
                        ),
                    ),
                    clock,
                )

            advanceUntilIdle()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(ExpensesIntent.Refresh)
            advanceUntilIdle()

            assertEquals(
                ExpensesUiState.Content(summary.total, summary.currencyCode, summary.transactions),
                viewModel.state.value,
            )
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

    @Test
    fun `selecting a future date keeps the current day`() =
        runTest {
            val repository =
                FakeFinanceRepository(
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = ExpensesViewModel(GetDailySummaryUseCase(repository), clock)

            advanceUntilIdle()
            viewModel.onIntent(ExpensesIntent.SelectDate(LocalDate.of(2026, 7, 18)))
            advanceUntilIdle()

            assertEquals(listOf(LocalDate.of(2026, 7, 17)), repository.requestedDates)
        }

    @Test
    fun `refresh reloads the current date`() =
        runTest {
            val repository =
                FakeFinanceRepository(
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = ExpensesViewModel(GetDailySummaryUseCase(repository), clock)

            advanceUntilIdle()
            viewModel.onIntent(ExpensesIntent.Refresh)
            advanceUntilIdle()

            assertEquals(listOf(LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)), repository.requestedDates)
        }
}

private class FakeFinanceRepository(
    vararg results: FinanceLoadResult,
) : FinanceRepository {
    private val results = ArrayDeque(results.toList())
    val requestedDates = mutableListOf<LocalDate>()

    override suspend fun getAccounts(): FinanceDataLoadResult<List<Account>> =
        when (val result = results.first()) {
            is FinanceLoadResult.Success -> {
                FinanceDataLoadResult.Success(
                    result.summary.transactions
                        .map { it.account }
                        .distinctBy { it.id },
                )
            }

            is FinanceLoadResult.Failure -> {
                FinanceDataLoadResult.Failure(result.reason)
            }
        }

    override suspend fun getTransactions(query: TransactionsQuery): FinanceDataLoadResult<List<Transaction>> {
        requestedDates += query.startDate
        return when (val result = results.removeFirst()) {
            is FinanceLoadResult.Success -> FinanceDataLoadResult.Success(result.summary.transactions)
            is FinanceLoadResult.Failure -> FinanceDataLoadResult.Failure(result.reason)
        }
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
