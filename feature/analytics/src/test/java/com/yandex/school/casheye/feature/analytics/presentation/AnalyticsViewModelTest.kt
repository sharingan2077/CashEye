package com.yandex.school.casheye.feature.analytics.presentation

import com.yandex.school.casheye.core.format.formatAmount
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.AnalyticsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsSummary
import com.yandex.school.casheye.domain.finance.AnalyticsTransaction
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAnalyticsUseCase
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
class AnalyticsViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-07-18T14:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `entry points select their expected initial type and current month`() =
        runTest {
            val repository = QueueAnalyticsRepository(success(), success(), success())

            listOf(
                AnalyticsEntryPoint.Expenses,
                AnalyticsEntryPoint.Income,
                AnalyticsEntryPoint.Accounts,
            ).forEach { entryPoint ->
                val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
                viewModel.onIntent(AnalyticsIntent.Initialize(entryPoint))
                advanceUntilIdle()
            }

            repository.queries.forEach { query ->
                assertEquals(LocalDate.of(2026, 7, 1), query.startDate)
                assertEquals(LocalDate.of(2026, 7, 18), query.endDate)
            }
        }

    @Test
    fun `calendar presets start on current calendar boundaries`() =
        runTest {
            val repository = QueueAnalyticsRepository(success(), success(), success(), success(), success())
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            listOf(
                AnalyticsPeriodPreset.Week,
                AnalyticsPeriodPreset.Month,
                AnalyticsPeriodPreset.Quarter,
                AnalyticsPeriodPreset.Year,
            ).forEach { preset ->
                viewModel.onIntent(AnalyticsIntent.SelectPeriodPreset(preset))
                advanceUntilIdle()
            }

            assertEquals(
                listOf(
                    LocalDate.of(2026, 7, 13),
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 1, 1),
                ),
                repository.queries.drop(1).map { it.startDate },
            )
            assertTrue(repository.queries.all { it.endDate == LocalDate.of(2026, 7, 18) })
        }

    @Test
    fun `custom period changes only after apply and dismiss keeps previous range`() =
        runTest {
            val repository = QueueAnalyticsRepository(success(), success())
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            viewModel.onIntent(AnalyticsIntent.OpenCustomPeriod)
            viewModel.onIntent(
                AnalyticsIntent.UpdateCustomPeriod(
                    LocalDate.of(2026, 6, 2),
                    LocalDate.of(2026, 6, 12),
                ),
            )
            viewModel.onIntent(AnalyticsIntent.DismissSheet)
            assertEquals(1, repository.queries.size)

            viewModel.onIntent(AnalyticsIntent.OpenCustomPeriod)
            viewModel.onIntent(
                AnalyticsIntent.UpdateCustomPeriod(
                    LocalDate.of(2026, 6, 2),
                    LocalDate.of(2026, 6, 12),
                ),
            )
            viewModel.onIntent(AnalyticsIntent.ApplyCustomPeriod)
            advanceUntilIdle()

            assertEquals(LocalDate.of(2026, 6, 2), repository.queries.last().startDate)
            assertEquals(LocalDate.of(2026, 6, 12), repository.queries.last().endDate)

            viewModel.onIntent(AnalyticsIntent.OpenCustomPeriod)
            viewModel.onIntent(
                AnalyticsIntent.UpdateCustomPeriod(
                    LocalDate.of(2026, 7, 19),
                    LocalDate.of(2026, 7, 20),
                ),
            )
            viewModel.onIntent(AnalyticsIntent.ApplyCustomPeriod)

            assertEquals(2, repository.queries.size)
        }

    @Test
    fun `type categories and account filters are sent only when applied`() =
        runTest {
            val repository =
                QueueAnalyticsRepository(
                    successWithOptions(),
                    successWithOptions(),
                    successWithOptions(),
                    successWithOptions(),
                )
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            viewModel.onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Type))
            viewModel.onIntent(AnalyticsIntent.SelectDraftType(AnalyticsType.All))
            assertEquals(1, repository.queries.size)
            viewModel.onIntent(AnalyticsIntent.ApplyDraftType)
            advanceUntilIdle()

            viewModel.onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Categories))
            viewModel.onIntent(AnalyticsIntent.ToggleDraftCategory(10))
            viewModel.onIntent(AnalyticsIntent.ApplyDraftCategories)
            advanceUntilIdle()

            viewModel.onIntent(AnalyticsIntent.OpenFilter(AnalyticsFilterKind.Account))
            viewModel.onIntent(AnalyticsIntent.SelectAccount(2))
            advanceUntilIdle()

            assertEquals(null, viewModel.state.value.data.activeSheet)
        }

    @Test
    fun `success sorts transactions and groups category amounts`() =
        runTest {
            val older = transaction(id = 1, categoryId = 10, amount = "5", date = "2026-07-02T10:00:00Z")
            val newer = transaction(id = 2, categoryId = 10, amount = "7", date = "2026-07-17T10:00:00Z")
            val repository = QueueAnalyticsRepository(success(transactions = listOf(older, newer)))
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)

            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            val state = viewModel.state.value as AnalyticsUiState.Content
            assertEquals(listOf(2, 1), state.transactions.map { it.id })
            assertEquals(BigDecimal("12"), state.categorySummaries.single().amount)
        }

    @Test
    fun `all type aggregates expenses and income after filtering`() {
        val transactions =
            listOf(
                transaction(id = 1, categoryId = 10, amount = "40"),
                transaction(id = 2, categoryId = 20, amount = "60", isIncome = true),
            )

        assertEquals(
            listOf(
                AnalyticsTypeSummary(AnalyticsType.Income, BigDecimal("60")),
                AnalyticsTypeSummary(AnalyticsType.Expenses, BigDecimal("40")),
            ),
            transactions.map { it.toAnalyticsTransaction() }.toTypeSummaries(),
        )
    }

    @Test
    fun `type aggregation hides zero groups and calculates single type totals`() {
        val expenses = listOf(transaction(id = 1, categoryId = 10, amount = "40"))
        val income = listOf(transaction(id = 2, categoryId = 20, amount = "60", isIncome = true))

        val analyticsExpenses = expenses.map { it.toAnalyticsTransaction() }
        val analyticsIncome = income.map { it.toAnalyticsTransaction() }
        assertEquals(listOf(AnalyticsType.Expenses), analyticsExpenses.toTypeSummaries().map { it.type })
        assertEquals(listOf(AnalyticsType.Income), analyticsIncome.toTypeSummaries().map { it.type })
        assertTrue(
            listOf(transaction(id = 3, categoryId = 30, amount = "0"))
                .map { it.toAnalyticsTransaction() }
                .toTypeSummaries()
                .isEmpty(),
        )
    }

    @Test
    fun `aggregations use reporting amount instead of original amount`() {
        val transaction =
            transaction(id = 1, categoryId = 10, amount = "10")
                .toAnalyticsTransaction()
                .copy(reportingAmount = MoneyAmount(BigDecimal("900"), CurrencyCode.RUB))
        val transactions = listOf(transaction)

        assertEquals(BigDecimal("900"), transactions.toTypeSummaries().single().amount)
    }

    @Test
    fun `amount signs appear only for all filter`() {
        assertEquals(BigDecimal("-15"), signedAnalyticsAmount(BigDecimal("15"), AnalyticsType.Expenses))
        assertEquals(BigDecimal("15"), signedAnalyticsAmount(BigDecimal("15"), AnalyticsType.Income))
        assertEquals(BigDecimal("-5"), signedAnalyticsAmount(BigDecimal("-5"), AnalyticsType.All))
        assertEquals(
            "+${formatAmount(BigDecimal("15"), "RUB")}",
            formatAnalyticsDisplayAmount(
                BigDecimal("15"),
                AnalyticsType.Income,
                AnalyticsType.All,
                "RUB",
            ),
        )
        assertEquals(
            "-${formatAmount(BigDecimal("15"), "RUB")}",
            formatAnalyticsDisplayAmount(
                BigDecimal("15"),
                AnalyticsType.Expenses,
                AnalyticsType.All,
                "RUB",
            ),
        )
        assertEquals(
            formatAmount(BigDecimal("15"), "RUB"),
            formatAnalyticsDisplayAmount(
                BigDecimal("15"),
                AnalyticsType.Expenses,
                AnalyticsType.Expenses,
                "RUB",
            ),
        )
        assertEquals(
            "+${formatAmount(BigDecimal("5"), "RUB")}",
            formatAnalyticsDisplayAmount(
                BigDecimal("5"),
                AnalyticsType.All,
                AnalyticsType.All,
                "RUB",
            ),
        )
        assertEquals(
            formatAmount(BigDecimal("-5"), "RUB"),
            formatAnalyticsDisplayAmount(
                BigDecimal("-5"),
                AnalyticsType.All,
                AnalyticsType.All,
                "RUB",
            ),
        )
        assertEquals(
            formatAmount(BigDecimal("15"), "RUB"),
            formatAnalyticsDisplayAmount(
                BigDecimal("15"),
                AnalyticsType.Income,
                AnalyticsType.Income,
                "RUB",
            ),
        )
        assertEquals(
            formatAmount(BigDecimal("15"), "RUB"),
            formatAnalyticsDisplayAmount(
                BigDecimal("-15"),
                AnalyticsType.All,
                AnalyticsType.Expenses,
                "RUB",
            ),
        )
    }

    @Test
    fun `empty result exposes empty state`() =
        runTest {
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(QueueAnalyticsRepository(success())), clock)

            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Empty)
        }

    @Test
    fun `initial failure exposes error and retry loads again`() =
        runTest {
            val repository =
                QueueAnalyticsRepository(
                    AnalyticsLoadResult.Failure(FinanceFailureReason.Network),
                    success(),
                )
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)

            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Error)

            viewModel.onIntent(AnalyticsIntent.Retry)
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Empty)
            assertEquals(2, repository.accountRequests)
            assertEquals(2, repository.queries.size)
        }

    @Test
    fun `failed refresh keeps content and emits show error effect`() =
        runTest {
            val repository =
                QueueAnalyticsRepository(
                    success(transactions = listOf(transaction(id = 1, categoryId = 10, amount = "3"))),
                    AnalyticsLoadResult.Failure(FinanceFailureReason.Server),
                )
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(AnalyticsIntent.Refresh)
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Content)
            assertEquals(AnalyticsEffect.ShowError(FinanceFailureReason.Server), effect.await())
        }

    @Test
    fun `refresh reloads the current analytics filters`() =
        runTest {
            val repository = QueueAnalyticsRepository(success(), success())
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            viewModel.onIntent(AnalyticsIntent.Refresh)
            advanceUntilIdle()

            assertEquals(2, repository.queries.size)
            assertEquals(repository.queries.first(), repository.queries.last())
        }

    @Test
    fun `cached analytics stays visible while initial refresh is running`() =
        runTest {
            val cached = transaction(id = 1, categoryId = 10, amount = "3")
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
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)

            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            runCurrent()

            val state = viewModel.state.value as AnalyticsUiState.Content
            assertTrue(state.isRefreshing)
            assertEquals(listOf(1), state.transactions.map { it.id })

            refresh.complete(FinanceRefreshResult.Success)
            advanceUntilIdle()
            assertEquals(false, (viewModel.state.value as AnalyticsUiState.Content).isRefreshing)
        }

    @Test
    fun `network failure waits for cached analytics without emitting error`() =
        runTest {
            val cached = transaction(id = 1, categoryId = 10, amount = "3")
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
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))

            runCurrent()
            assertTrue(viewModel.state.value is AnalyticsUiState.Loading)

            val effects = mutableListOf<AnalyticsEffect>()
            val collector =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.effects.collect { effects += it }
                }
            cacheReady.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Content)
            assertTrue(effects.isEmpty())
            collector.cancel()
        }

    @Test
    fun `offline refresh exposes empty analytics when cache is initialized`() =
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
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            val effects = mutableListOf<AnalyticsEffect>()
            val collector =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.effects.collect { effects += it }
                }

            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Empty)
            assertTrue(effects.isEmpty())
            collector.cancel()
        }

    @Test
    fun `room emission keeps the active analytics sheet open`() =
        runTest {
            val repository = QueueAnalyticsRepository(successWithOptions())
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            viewModel.onIntent(AnalyticsIntent.OpenDetails)
            repository.emit(success(transactions = listOf(transaction(id = 2, categoryId = 20, amount = "9"))))
            runCurrent()

            assertEquals(AnalyticsSheet.Details, viewModel.state.value.data.activeSheet)
            assertEquals(listOf(2), (viewModel.state.value as AnalyticsUiState.Content).transactions.map { it.id })
        }

    @Test
    fun `new filter cancels stale request before it can replace current state`() =
        runTest {
            val firstStarted = CompletableDeferred<Unit>()
            val firstResult = CompletableDeferred<AnalyticsLoadResult>()
            val repository =
                LambdaAnalyticsRepository { _, requestIndex ->
                    if (requestIndex == 0) {
                        firstStarted.complete(Unit)
                        firstResult.await()
                    } else {
                        success(transactions = listOf(transaction(id = 2, categoryId = 20, amount = "9")))
                    }
                }
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            runCurrent()
            firstStarted.await()

            viewModel.onIntent(AnalyticsIntent.SelectPeriodPreset(AnalyticsPeriodPreset.Week))
            advanceUntilIdle()
            firstResult.complete(success(transactions = listOf(transaction(id = 1, categoryId = 10, amount = "4"))))
            advanceUntilIdle()

            val state = viewModel.state.value as AnalyticsUiState.Content
            assertEquals(listOf(2), state.transactions.map { it.id })
            assertEquals(2, repository.queries.size)
        }
}

private open class StubFinanceRepository : FinanceRepository {
    override suspend fun getAccounts() = error("Not used")

    override suspend fun getTransactions(query: TransactionsQuery) = error("Not used")
}

private class QueueAnalyticsRepository(
    vararg results: AnalyticsLoadResult,
) : StubFinanceRepository() {
    private val results = ArrayDeque(results.toList())
    private val firstSummary = (results.firstOrNull() as? AnalyticsLoadResult.Success)?.summary
    private val accounts = MutableStateFlow(firstSummary?.accounts.orEmpty())
    private val transactions =
        MutableStateFlow(firstSummary?.transactions.orEmpty().map { it.transaction })
    private var observedQuery = TransactionsQuery(emptySet(), LocalDate.MIN, LocalDate.MIN)
    val queries = mutableListOf<TransactionsQuery>()
    var accountRequests = 0

    override fun observeAccounts(): Flow<List<Account>> = accounts

    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> {
        observedQuery = query
        return transactions
    }

    override suspend fun refreshPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
    ): FinanceRefreshResult {
        accountRequests += 1
        queries += observedQuery
        return when (val result = results.removeFirst()) {
            is AnalyticsLoadResult.Success -> {
                accounts.value = result.summary.accounts
                transactions.value = result.summary.transactions.map { it.transaction }
                FinanceRefreshResult.Success
            }

            is AnalyticsLoadResult.Failure -> {
                FinanceRefreshResult.Failure(result.reason, hasUsableCache = false)
            }
        }
    }

    fun emit(result: AnalyticsLoadResult.Success) {
        accounts.value = result.summary.accounts
        transactions.value = result.summary.transactions.map { it.transaction }
    }
}

private class LambdaAnalyticsRepository(
    private val block: suspend (TransactionsQuery, Int) -> AnalyticsLoadResult,
) : StubFinanceRepository() {
    val queries = mutableListOf<TransactionsQuery>()
    private val accounts = MutableStateFlow(listOf(account(1), account(2)))
    private val transactions = MutableStateFlow<List<Transaction>>(emptyList())
    private var observedQuery = TransactionsQuery(emptySet(), LocalDate.MIN, LocalDate.MIN)

    override fun observeAccounts(): Flow<List<Account>> = accounts

    override fun observeTransactions(query: TransactionsQuery): Flow<List<Transaction>> {
        observedQuery = query
        return transactions
    }

    override suspend fun refreshPeriod(
        startDate: LocalDate,
        endDate: LocalDate,
    ): FinanceRefreshResult {
        val index = queries.size
        queries += observedQuery
        return when (val result = block(observedQuery, index)) {
            is AnalyticsLoadResult.Success -> {
                accounts.value = result.summary.accounts
                transactions.value = result.summary.transactions.map { it.transaction }
                FinanceRefreshResult.Success
            }

            is AnalyticsLoadResult.Failure -> {
                FinanceRefreshResult.Failure(result.reason, hasUsableCache = false)
            }
        }
    }
}

private fun success(transactions: List<Transaction> = emptyList()): AnalyticsLoadResult.Success {
    val analyticsTransactions = transactions.map { it.toAnalyticsTransaction() }
    return AnalyticsLoadResult.Success(
        AnalyticsSummary(
            total =
                analyticsTransactions.fold(BigDecimal.ZERO) { total, transaction ->
                    total + transaction.reportingAmount.amount
                },
            currencyCode = CurrencyCode.RUB,
            transactions = analyticsTransactions,
            unconvertedTransactions = emptyList(),
            accounts = emptyList(),
            availableCategories = transactions.map { it.category }.distinctBy { it.id },
        ),
    )
}

private fun successWithOptions(): AnalyticsLoadResult.Success {
    val transaction = transaction(id = 1, categoryId = 10, amount = "3")
    val analyticsTransaction = transaction.toAnalyticsTransaction()
    return AnalyticsLoadResult.Success(
        AnalyticsSummary(
            total = analyticsTransaction.reportingAmount.amount,
            currencyCode = CurrencyCode.RUB,
            transactions = listOf(analyticsTransaction),
            unconvertedTransactions = emptyList(),
            accounts = listOf(account(1), account(2)),
            availableCategories = listOf(transaction.category),
        ),
    )
}

private fun transaction(
    id: Int,
    categoryId: Int,
    amount: String,
    date: String = "2026-07-17T10:00:00Z",
    isIncome: Boolean = false,
): Transaction {
    val instant = Instant.parse(date)
    return Transaction(
        id = id,
        account = account(1),
        category = Category(categoryId, "Категория $categoryId", "🛒", isIncome),
        amount = BigDecimal(amount),
        transactionDate = instant,
        comment = null,
        createdAt = instant,
        updatedAt = instant,
    )
}

private fun account(id: Int): Account =
    Account(
        id = id,
        name = "Счёт $id",
        emoji = "💳",
        balance = BigDecimal("100"),
        currency = "RUB",
    )

private fun Transaction.toAnalyticsTransaction(): AnalyticsTransaction =
    AnalyticsTransaction(
        transaction = this,
        originalAmount = MoneyAmount(amount, currency),
        reportingAmount = MoneyAmount(amount, CurrencyCode.RUB),
        rateDate = null,
    )
