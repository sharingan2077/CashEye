package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.domain.expenses.ExpensesLoadResult
import com.yandex.school.casheye.domain.expenses.ExpensesRepository
import com.yandex.school.casheye.domain.expenses.ExpensesSummary
import com.yandex.school.casheye.domain.expenses.GetExpensesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesDateSelectionViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()
    private val currentDate = LocalDate.of(2026, 7, 17)
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
    fun `selecting date reloads expenses for selected day`() =
        runTest {
            val selectedDate = LocalDate.of(2026, 6, 18)
            val repository = RecordingExpensesRepository()
            val viewModel = ExpensesViewModel(GetExpensesUseCase(repository), clock)
            advanceUntilIdle()

            viewModel.onIntent(ExpensesIntent.SelectDate(selectedDate))
            advanceUntilIdle()

            assertEquals(listOf(currentDate, selectedDate), repository.requestedDates)
        }
}

private class RecordingExpensesRepository : ExpensesRepository {
    val requestedDates = mutableListOf<LocalDate>()

    override suspend fun getExpenses(
        date: LocalDate,
        currencyCode: String,
    ): ExpensesLoadResult {
        requestedDates += date
        return ExpensesLoadResult.Success(
            ExpensesSummary(
                total = BigDecimal.ZERO,
                currencyCode = currencyCode,
                transactions = emptyList(),
            ),
        )
    }
}
