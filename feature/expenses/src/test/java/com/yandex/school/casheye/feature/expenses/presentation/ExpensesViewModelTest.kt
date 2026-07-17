package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.expenses.ExpensesFailureReason
import com.yandex.school.casheye.domain.expenses.GetExpensesUseCase
import com.yandex.school.casheye.domain.expenses.ExpensesLoadResult
import com.yandex.school.casheye.domain.expenses.ExpensesRepository
import com.yandex.school.casheye.domain.expenses.ExpensesSummary
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
    fun `successful load exposes content`() =
        runTest {
            val summary =
                ExpensesSummary(
                    total = BigDecimal("25.00"),
                    currencyCode = "RUB",
                    transactions = listOf(transaction()),
                )
            val viewModel =
                ExpensesViewModel(
                    getExpenses = GetExpensesUseCase(FakeExpensesRepository(ExpensesLoadResult.Success(summary))),
                    clock = clock,
                )

            advanceUntilIdle()

            assertEquals(
                ExpensesUiState.Content(
                    total = summary.total,
                    currencyCode = summary.currencyCode,
                    transactions = summary.transactions,
                ),
                viewModel.state.value,
            )
        }

    @Test
    fun `successful empty load exposes empty state`() =
        runTest {
            val viewModel =
                ExpensesViewModel(
                    getExpenses =
                        GetExpensesUseCase(FakeExpensesRepository(
                            ExpensesLoadResult.Success(
                                ExpensesSummary(BigDecimal.ZERO, "RUB", emptyList()),
                            ),
                        )),
                    clock = clock,
                )

            advanceUntilIdle()

            assertEquals(ExpensesUiState.Empty, viewModel.state.value)
        }

    @Test
    fun `failure exposes error and emits show error effect`() =
        runTest {
            val viewModel =
                ExpensesViewModel(
                    getExpenses =
                        GetExpensesUseCase(FakeExpensesRepository(
                            ExpensesLoadResult.Failure(ExpensesFailureReason.Network),
                        )),
                    clock = clock,
                )
            val effect =
                async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.effects.first()
                }

            advanceUntilIdle()

            assertTrue(viewModel.state.value is ExpensesUiState.Error)
            assertEquals(
                ExpensesEffect.ShowError("Проверьте подключение к интернету"),
                effect.await(),
            )
        }

    @Test
    fun `retry performs another load`() =
        runTest {
            val repository =
                FakeExpensesRepository(
                    ExpensesLoadResult.Failure(ExpensesFailureReason.Server),
                    ExpensesLoadResult.Success(
                        ExpensesSummary(BigDecimal("25.00"), "RUB", listOf(transaction())),
                    ),
                )
            val viewModel = ExpensesViewModel(GetExpensesUseCase(repository), clock)
            advanceUntilIdle()

            viewModel.onIntent(ExpensesIntent.Retry)
            advanceUntilIdle()

            assertEquals(2, repository.requestedDates.size)
            assertTrue(viewModel.state.value is ExpensesUiState.Content)
        }

    @Test
    fun `load requests the current day from injected clock`() =
        runTest {
            val repository =
                FakeExpensesRepository(
                    ExpensesLoadResult.Success(
                        ExpensesSummary(BigDecimal.ZERO, "RUB", emptyList()),
                    ),
                )

            ExpensesViewModel(GetExpensesUseCase(repository), clock)
            advanceUntilIdle()

            assertEquals(listOf(LocalDate.of(2026, 7, 17)), repository.requestedDates)
            assertEquals(listOf("RUB"), repository.requestedCurrencies)
        }
}

private class FakeExpensesRepository(
    vararg results: ExpensesLoadResult,
) : ExpensesRepository {
    private val results = ArrayDeque(results.toList())
    val requestedDates = mutableListOf<LocalDate>()
    val requestedCurrencies = mutableListOf<String>()

    override suspend fun getExpenses(
        date: LocalDate,
        currencyCode: String,
    ): ExpensesLoadResult {
        requestedDates += date
        requestedCurrencies += currencyCode
        return results.removeFirst()
    }
}

private fun transaction(): Transaction {
    val instant = Instant.parse("2026-07-17T10:00:00Z")
    return Transaction(
        id = 1,
        account = Account(1, "Основной счёт", BigDecimal("1000.00"), "RUB"),
        category = Category(1, "Продукты", "🛒", false),
        amount = BigDecimal("25.00"),
        transactionDate = instant,
        comment = null,
        createdAt = instant,
        updatedAt = instant,
    )
}
