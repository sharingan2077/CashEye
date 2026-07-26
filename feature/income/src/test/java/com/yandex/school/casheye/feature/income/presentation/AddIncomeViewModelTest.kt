package com.yandex.school.casheye.feature.income.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceDataLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetEditorAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetEditorCategoriesUseCase
import com.yandex.school.casheye.domain.finance.GetTransactionUseCase
import com.yandex.school.casheye.domain.finance.SaveTransactionCommand
import com.yandex.school.casheye.domain.finance.SaveTransactionUseCase
import com.yandex.school.casheye.domain.finance.TransactionsQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class AddIncomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `new income loads income categories and saves transaction`() =
        runTest {
            val repository = IncomeEditorRepository()
            val viewModel =
                AddIncomeViewModel(
                    GetEditorAccountsUseCase(repository),
                    GetEditorCategoriesUseCase(repository),
                    GetTransactionUseCase(repository),
                    SaveTransactionUseCase(repository),
                    clock,
                )
            viewModel.onIntent(AddIncomeIntent.Open(null, LocalDate.of(2026, 7, 22)))
            advanceUntilIdle()
            viewModel.onIntent(AddIncomeIntent.CategorySelected(9))
            viewModel.onIntent(AddIncomeIntent.AmountChanged("500"))
            viewModel.onIntent(AddIncomeIntent.Save)
            advanceUntilIdle()

            assertTrue(repository.requestedIncome)
            assertEquals(BigDecimal("500"), repository.saved?.amount)
        }

    @Test
    fun `income editor clamps future dates to today`() =
        runTest {
            val repository = IncomeEditorRepository()
            val viewModel =
                AddIncomeViewModel(
                    GetEditorAccountsUseCase(repository),
                    GetEditorCategoriesUseCase(repository),
                    GetTransactionUseCase(repository),
                    SaveTransactionUseCase(repository),
                    clock,
                )

            viewModel.onIntent(AddIncomeIntent.Open(null, LocalDate.of(2026, 7, 25)))
            advanceUntilIdle()
            assertEquals(LocalDate.of(2026, 7, 22), viewModel.state.value.date)

            viewModel.onIntent(AddIncomeIntent.DateChanged(LocalDate.of(2026, 8, 1)))
            assertEquals(LocalDate.of(2026, 7, 22), viewModel.state.value.date)
        }
}

private class IncomeEditorRepository : FinanceRepository {
    var requestedIncome = false
    var saved: SaveTransactionCommand? = null

    override suspend fun getAccounts() =
        FinanceDataLoadResult.Success(listOf(Account(1, "Основной", "💵", BigDecimal.ZERO, "RUB")))

    override suspend fun getTransactions(query: TransactionsQuery) = error("Not used")

    override suspend fun getCategories(isIncome: Boolean): EditorResult<List<Category>> {
        requestedIncome = isIncome
        return EditorResult.Success(listOf(Category(9, "Зарплата", "💰", true)))
    }

    override suspend fun saveTransaction(command: SaveTransactionCommand): EditorResult<Unit> {
        saved = command
        return EditorResult.Success(Unit)
    }
}
