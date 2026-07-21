package com.yandex.school.casheye.feature.analytics.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsQuery
import com.yandex.school.casheye.domain.finance.AnalyticsSummary
import com.yandex.school.casheye.domain.finance.AnalyticsTransactionKind
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAnalyticsUseCase
import com.yandex.school.casheye.domain.finance.TransactionKind
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
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

            assertEquals(
                listOf(
                    AnalyticsTransactionKind.Expense,
                    AnalyticsTransactionKind.Income,
                    AnalyticsTransactionKind.Expense,
                ),
                repository.queries.map { it.transactionKind },
            )
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

            assertEquals(AnalyticsTransactionKind.All, repository.queries[1].transactionKind)
            assertEquals(setOf(10), repository.queries[2].categoryIds)
            assertEquals(2, repository.queries[3].accountId)
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
    fun `empty result exposes empty state`() =
        runTest {
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(QueueAnalyticsRepository(success())), clock)

            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Empty)
        }

    @Test
    fun `failure emits effect and retry loads again`() =
        runTest {
            val repository =
                QueueAnalyticsRepository(
                    AnalyticsLoadResult.Failure(FinanceFailureReason.Network),
                    success(),
                )
            val viewModel = AnalyticsViewModel(GetAnalyticsUseCase(repository), clock)
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

            viewModel.onIntent(AnalyticsIntent.Initialize(AnalyticsEntryPoint.Expenses))
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Error)
            assertEquals(AnalyticsEffect.ShowError(FinanceFailureReason.Network), effect.await())

            viewModel.onIntent(AnalyticsIntent.Retry)
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AnalyticsUiState.Empty)
            assertEquals(2, repository.queries.size)
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

private class QueueAnalyticsRepository(
    vararg results: AnalyticsLoadResult,
) : FinanceRepository {
    private val results = ArrayDeque(results.toList())
    val queries = mutableListOf<AnalyticsQuery>()

    override suspend fun getAnalytics(query: AnalyticsQuery): AnalyticsLoadResult {
        queries += query
        return results.removeFirst()
    }

    override suspend fun getDailySummary(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult = error("Not used")

    override suspend fun getAccountsSummary(currencyCode: String): AccountsLoadResult = error("Not used")
}

private class LambdaAnalyticsRepository(
    private val block: suspend (AnalyticsQuery, Int) -> AnalyticsLoadResult,
) : FinanceRepository {
    val queries = mutableListOf<AnalyticsQuery>()

    override suspend fun getAnalytics(query: AnalyticsQuery): AnalyticsLoadResult {
        val index = queries.size
        queries += query
        return block(query, index)
    }

    override suspend fun getDailySummary(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult = error("Not used")

    override suspend fun getAccountsSummary(currencyCode: String): AccountsLoadResult = error("Not used")
}

private fun success(transactions: List<Transaction> = emptyList()): AnalyticsLoadResult =
    AnalyticsLoadResult.Success(
        AnalyticsSummary(
            total = transactions.fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount },
            currencyCode = "RUB",
            transactions = transactions,
            accounts = emptyList(),
            availableCategories = transactions.map { it.category }.distinctBy { it.id },
        ),
    )

private fun successWithOptions(): AnalyticsLoadResult {
    val transaction = transaction(id = 1, categoryId = 10, amount = "3")
    return AnalyticsLoadResult.Success(
        AnalyticsSummary(
            total = transaction.amount,
            currencyCode = "RUB",
            transactions = listOf(transaction),
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
): Transaction {
    val instant = Instant.parse(date)
    return Transaction(
        id = id,
        account = account(1),
        category = Category(categoryId, "Категория $categoryId", "🛒", false),
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
