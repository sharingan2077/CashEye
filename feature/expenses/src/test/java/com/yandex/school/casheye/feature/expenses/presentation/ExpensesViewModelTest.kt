package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.DeleteTransactionUseCase
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.FinanceSummary
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.TransactionsQuery
import com.yandex.school.casheye.domain.finance.editor.EditorResult
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
            val summary =
                FinanceSummary(
                    nativeTotals = listOf(MoneyAmount(BigDecimal("25.00"), CurrencyCode.RUB)),
                    transactions = listOf(transaction()),
                )
            val repository = FakeFinanceRepository(FinanceLoadResult.Success(summary))
            val viewModel = expensesViewModel(repository, clock)

            advanceUntilIdle()

            assertEquals(
                ExpensesUiState.Content(summary.nativeTotals, summary.transactions),
                viewModel.state.value,
            )
        }

    @Test
    fun `empty load exposes empty state`() =
        runTest {
            val viewModel =
                expensesViewModel(
                    FakeFinanceRepository(
                        FinanceLoadResult.Success(FinanceSummary(emptyList(), emptyList())),
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
                expensesViewModel(
                    FakeFinanceRepository(FinanceLoadResult.Failure(FinanceFailureReason.Network)),
                    clock,
                )

            advanceUntilIdle()

            assertTrue(viewModel.state.value is ExpensesUiState.Error)
        }

    @Test
    fun `failed refresh keeps content and emits show error effect`() =
        runTest {
            val summary =
                FinanceSummary(
                    nativeTotals = listOf(MoneyAmount(BigDecimal("25.00"), CurrencyCode.RUB)),
                    transactions = listOf(transaction()),
                )
            val viewModel =
                expensesViewModel(
                    FakeFinanceRepository(
                        FinanceLoadResult.Success(summary),
                        FinanceLoadResult.Failure(FinanceFailureReason.Server),
                    ),
                    clock,
                )

            advanceUntilIdle()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(ExpensesIntent.Refresh)
            advanceUntilIdle()

            assertEquals(
                ExpensesUiState.Content(summary.nativeTotals, summary.transactions),
                viewModel.state.value,
            )
            assertEquals(ExpensesEffect.ShowError(FinanceFailureReason.Server), effect.await())
        }

    @Test
    fun `selecting date reloads the selected day`() =
        runTest {
            val repository =
                FakeFinanceRepository(
                    FinanceLoadResult.Success(FinanceSummary(emptyList(), emptyList())),
                    FinanceLoadResult.Success(FinanceSummary(emptyList(), emptyList())),
                )
            val viewModel = expensesViewModel(repository, clock)
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
                    FinanceLoadResult.Success(FinanceSummary(emptyList(), emptyList())),
                )
            val viewModel = expensesViewModel(repository, clock)

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
                    FinanceLoadResult.Success(FinanceSummary(emptyList(), emptyList())),
                    FinanceLoadResult.Success(FinanceSummary(emptyList(), emptyList())),
                )
            val viewModel = expensesViewModel(repository, clock)

            advanceUntilIdle()
            viewModel.onIntent(ExpensesIntent.Refresh)
            advanceUntilIdle()

            assertEquals(listOf(LocalDate.of(2026, 7, 17), LocalDate.of(2026, 7, 17)), repository.requestedDates)
        }

    @Test
    fun `network recovery shows loading after an empty-cache error`() =
        runTest {
            val refresh = CompletableDeferred<FinanceRefreshResult>()
            var refreshCount = 0
            val repository =
                object : StubFinanceRepository() {
                    override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(emptyList())

                    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> =
                        MutableStateFlow(emptyList())

                    override suspend fun refreshPeriod(
                        startDate: LocalDate,
                        endDate: LocalDate,
                    ): FinanceRefreshResult =
                        if (++refreshCount == 1) {
                            FinanceRefreshResult.Failure(FinanceFailureReason.Network, hasUsableCache = false)
                        } else {
                            refresh.await()
                        }
                }
            val viewModel = expensesViewModel(repository, clock)

            advanceUntilIdle()
            assertEquals(ExpensesUiState.Error(FinanceFailureReason.Network), viewModel.state.value)

            viewModel.onIntent(ExpensesIntent.NetworkRecovered)
            runCurrent()
            assertEquals(ExpensesUiState.Loading, viewModel.state.value)

            refresh.complete(FinanceRefreshResult.Success)
            advanceUntilIdle()
            assertEquals(ExpensesUiState.Empty(), viewModel.state.value)
        }

    @Test
    fun `cached expenses stay visible while initial refresh is running`() =
        runTest {
            val cached = transaction()
            val refresh = CompletableDeferred<FinanceRefreshResult>()
            val repository =
                object : StubFinanceRepository() {
                    override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(listOf(cached.account))

                    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> =
                        MutableStateFlow(listOf(cached))

                    override suspend fun refreshPeriod(
                        startDate: LocalDate,
                        endDate: LocalDate,
                    ): FinanceRefreshResult = refresh.await()
                }
            val viewModel = expensesViewModel(repository, clock)

            runCurrent()

            val state = viewModel.state.value as ExpensesUiState.Content
            assertTrue(state.isRefreshing)
            assertEquals(listOf(1), state.transactions.map { it.id })

            refresh.complete(FinanceRefreshResult.Success)
            advanceUntilIdle()
            assertEquals(false, (viewModel.state.value as ExpensesUiState.Content).isRefreshing)
        }

    @Test
    fun `network failure waits for cached expenses without emitting error`() =
        runTest {
            val cached = transaction()
            val cacheReady = CompletableDeferred<Unit>()
            val repository =
                object : StubFinanceRepository() {
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
                    ): FinanceRefreshResult =
                        FinanceRefreshResult.Failure(
                            FinanceFailureReason.Network,
                            hasUsableCache = true,
                        )
                }
            val viewModel = expensesViewModel(repository, clock)

            runCurrent()
            assertEquals(ExpensesUiState.Loading, viewModel.state.value)

            val effects = mutableListOf<ExpensesEffect>()
            val collector =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.effects.collect { effects += it }
                }
            cacheReady.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.state.value is ExpensesUiState.Content)
            assertTrue(effects.isEmpty())
            collector.cancel()
        }

    @Test
    fun `offline refresh exposes empty state when cache is initialized`() =
        runTest {
            val repository =
                object : StubFinanceRepository() {
                    override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(emptyList())

                    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> =
                        MutableStateFlow(emptyList())

                    override suspend fun refreshPeriod(
                        startDate: LocalDate,
                        endDate: LocalDate,
                    ): FinanceRefreshResult =
                        FinanceRefreshResult.Failure(
                            FinanceFailureReason.Network,
                            hasUsableCache = true,
                        )
                }

            val viewModel = expensesViewModel(repository, clock)
            val effects = mutableListOf<ExpensesEffect>()
            val collector =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.effects.collect { effects += it }
                }
            advanceUntilIdle()

            assertEquals(ExpensesUiState.Empty(), viewModel.state.value)
            assertTrue(effects.isEmpty())
            collector.cancel()
        }

    @Test
    fun `deleting expense removes it and emits success effect`() =
        runTest {
            val summary =
                FinanceSummary(
                    nativeTotals = listOf(MoneyAmount(BigDecimal("25.00"), CurrencyCode.RUB)),
                    transactions = listOf(transaction()),
                )
            val repository = FakeFinanceRepository(FinanceLoadResult.Success(summary))
            val viewModel = expensesViewModel(repository, clock)

            advanceUntilIdle()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(ExpensesIntent.DeleteTransaction(1))
            advanceUntilIdle()

            assertEquals(listOf(1), repository.deletedTransactionIds)
            assertEquals(ExpensesUiState.Empty(), viewModel.state.value)
            assertEquals(ExpensesEffect.TransactionDeleted, effect.await())
        }
}

private fun expensesViewModel(
    repository: FinanceRepository,
    clock: Clock,
): ExpensesViewModel =
    ExpensesViewModel(
        GetDailySummaryUseCase(repository),
        DeleteTransactionUseCase(repository),
        clock,
    )

private open class StubFinanceRepository : FinanceRepository {
    override suspend fun getAccounts() = error("Not used")

    override suspend fun getTransactions(query: TransactionsQuery) = error("Not used")
}

private class FakeFinanceRepository(
    vararg results: FinanceLoadResult,
) : StubFinanceRepository() {
    private val results = ArrayDeque(results.toList())
    private val firstSummary = (results.firstOrNull() as? FinanceLoadResult.Success)?.summary
    private val accounts =
        MutableStateFlow(
            firstSummary
                ?.transactions
                .orEmpty()
                .map { it.account }
                .distinctBy { it.id },
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
                accounts.value =
                    result.summary.transactions
                        .map { it.account }
                        .distinctBy { it.id }
                transactions.value = result.summary.transactions
                FinanceRefreshResult.Success
            }

            is FinanceLoadResult.Failure -> {
                FinanceRefreshResult.Failure(result.reason, hasUsableCache = false)
            }
        }
    }

    override suspend fun deleteTransaction(id: Int): EditorResult<Unit> {
        deletedTransactionIds += id
        transactions.value = transactions.value.filterNot { it.id == id }
        return EditorResult.Success(Unit)
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
