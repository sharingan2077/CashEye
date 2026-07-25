package com.yandex.school.casheye.feature.income.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.DeleteTransactionUseCase
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.FinanceSummary
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.TransactionsQuery
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
            val viewModel = incomeViewModel(repository, clock)

            advanceUntilIdle()

            assertEquals(
                IncomeUiState.Content(summary.total, summary.currencyCode, summary.transactions),
                viewModel.state.value,
            )
        }

    @Test
    fun `empty income load exposes empty state`() =
        runTest {
            val viewModel =
                incomeViewModel(
                    FakeIncomeFinanceRepository(
                        FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                    ),
                    clock,
                )

            advanceUntilIdle()

            assertEquals(IncomeUiState.Empty(), viewModel.state.value)
        }

    @Test
    fun `initial failure exposes error state`() =
        runTest {
            val viewModel =
                incomeViewModel(
                    FakeIncomeFinanceRepository(FinanceLoadResult.Failure(FinanceFailureReason.Network)),
                    clock,
                )

            advanceUntilIdle()

            assertTrue(viewModel.state.value is IncomeUiState.Error)
        }

    @Test
    fun `failed refresh keeps content and emits show error effect`() =
        runTest {
            val summary = FinanceSummary(BigDecimal("125000.00"), "RUB", listOf(incomeTransaction()))
            val viewModel =
                incomeViewModel(
                    FakeIncomeFinanceRepository(
                        FinanceLoadResult.Success(summary),
                        FinanceLoadResult.Failure(FinanceFailureReason.Network),
                    ),
                    clock,
                )

            advanceUntilIdle()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(IncomeIntent.Refresh)
            advanceUntilIdle()

            assertEquals(
                IncomeUiState.Content(summary.total, summary.currencyCode, summary.transactions),
                viewModel.state.value,
            )
            assertEquals(IncomeEffect.ShowError(FinanceFailureReason.Network), effect.await())
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
            val viewModel = incomeViewModel(repository, clock)
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
        }

    @Test
    fun `selecting a future date keeps the current income day`() =
        runTest {
            val repository =
                FakeIncomeFinanceRepository(
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = incomeViewModel(repository, clock)

            advanceUntilIdle()
            viewModel.onIntent(IncomeIntent.SelectDate(LocalDate.of(2026, 7, 18)))
            advanceUntilIdle()

            assertEquals(listOf(LocalDate.of(2026, 7, 17)), repository.requestedDates)
        }

    @Test
    fun `refresh reloads the current date as income`() =
        runTest {
            val repository =
                FakeIncomeFinanceRepository(
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                    FinanceLoadResult.Success(FinanceSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = incomeViewModel(repository, clock)

            advanceUntilIdle()
            viewModel.onIntent(IncomeIntent.Refresh)
            advanceUntilIdle()

            assertEquals(listOf(LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)), repository.requestedDates)
        }

    @Test
    fun `cached income stays visible while initial refresh is running`() =
        runTest {
            val cached = incomeTransaction()
            val refresh = CompletableDeferred<FinanceRefreshResult>()
            val repository =
                object : FinanceRepository {
                    override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(listOf(cached.account))

                    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> =
                        MutableStateFlow(listOf(cached))

                    override suspend fun refreshPeriod(
                        startDate: LocalDate,
                        endDate: LocalDate,
                    ): FinanceRefreshResult = refresh.await()
                }
            val viewModel = incomeViewModel(repository, clock)

            runCurrent()

            val state = viewModel.state.value as IncomeUiState.Content
            assertTrue(state.isRefreshing)
            assertEquals(listOf(1), state.transactions.map { it.id })

            refresh.complete(FinanceRefreshResult.Success)
            advanceUntilIdle()
            assertEquals(false, (viewModel.state.value as IncomeUiState.Content).isRefreshing)
        }

    @Test
    fun `network failure waits for cached income before deciding error`() =
        runTest {
            val cached = incomeTransaction()
            val cacheReady = CompletableDeferred<Unit>()
            val repository =
                object : FinanceRepository {
                    override fun observeAccounts(): Flow<List<Account>> =
                        flow {
                            cacheReady.await()
                            emit(listOf(cached.account))
                        }

                    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> =
                        flow {
                            cacheReady.await()
                            emit(listOf(cached))
                        }

                    override suspend fun refreshPeriod(
                        startDate: LocalDate,
                        endDate: LocalDate,
                    ): FinanceRefreshResult = FinanceRefreshResult.Failure(FinanceFailureReason.Network)
                }
            val viewModel = incomeViewModel(repository, clock)

            runCurrent()
            assertEquals(IncomeUiState.Loading, viewModel.state.value)

            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            cacheReady.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.state.value is IncomeUiState.Content)
            assertEquals(IncomeEffect.ShowError(FinanceFailureReason.Network), effect.await())
        }

    @Test
    fun `deleting income removes it and emits success effect`() =
        runTest {
            val summary = FinanceSummary(BigDecimal("125000.00"), "RUB", listOf(incomeTransaction()))
            val repository = FakeIncomeFinanceRepository(FinanceLoadResult.Success(summary))
            val viewModel = incomeViewModel(repository, clock)

            advanceUntilIdle()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(IncomeIntent.DeleteTransaction(1))
            advanceUntilIdle()

            assertEquals(listOf(1), repository.deletedTransactionIds)
            assertEquals(IncomeUiState.Empty(), viewModel.state.value)
            assertEquals(IncomeEffect.TransactionDeleted, effect.await())
        }
}

private fun incomeViewModel(
    repository: FinanceRepository,
    clock: Clock,
): IncomeViewModel =
    IncomeViewModel(
        GetDailySummaryUseCase(repository),
        DeleteTransactionUseCase(repository),
        clock,
    )

private class FakeIncomeFinanceRepository(
    vararg results: FinanceLoadResult,
) : FinanceRepository {
    private val results = ArrayDeque(results.toList())
    private val firstSummary = (results.firstOrNull() as? FinanceLoadResult.Success)?.summary
    private val accounts =
        MutableStateFlow(
            firstSummary?.transactions.orEmpty().map { it.account }.distinctBy { it.id },
        )
    private val transactions = MutableStateFlow(firstSummary?.transactions.orEmpty())
    val requestedDates = mutableListOf<LocalDate>()
    val deletedTransactionIds = mutableListOf<Int>()

    override fun observeAccounts(): Flow<List<Account>> = accounts

    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> = transactions

    override suspend fun refreshPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
    ): FinanceRefreshResult {
        requestedDates += startDate
        return when (val result = results.removeFirst()) {
            is FinanceLoadResult.Success -> {
                accounts.value = result.summary.transactions.map { it.account }.distinctBy { it.id }
                transactions.value = result.summary.transactions
                FinanceRefreshResult.Success
            }

            is FinanceLoadResult.Failure -> FinanceRefreshResult.Failure(result.reason)
        }
    }

    override suspend fun deleteTransaction(id: Int): EditorResult<Unit> {
        deletedTransactionIds += id
        transactions.value = transactions.value.filterNot { it.id == id }
        return EditorResult.Success(Unit)
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
