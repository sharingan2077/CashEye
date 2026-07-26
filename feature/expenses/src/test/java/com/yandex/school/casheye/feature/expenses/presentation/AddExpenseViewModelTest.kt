package com.yandex.school.casheye.feature.expenses.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceDataLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetEditorAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetEditorCategoriesUseCase
import com.yandex.school.casheye.domain.finance.GetTransactionUseCase
import com.yandex.school.casheye.domain.finance.SaveTransactionCommand
import com.yandex.school.casheye.domain.finance.SaveTransactionUseCase
import com.yandex.school.casheye.domain.finance.TransactionsQuery
import com.yandex.school.casheye.feature.expenses.R
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val clock = Clock.fixed(Instant.parse("2026-07-22T10:30:00Z"), ZoneOffset.UTC)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `expense that would make balance negative is not saved`() =
        runTest {
            val repository = EditorRepository(accounts = listOf(account(balance = "50")))
            val viewModel = viewModel(repository)
            viewModel.onIntent(AddExpenseIntent.Open(null, LocalDate.of(2026, 7, 22)))
            advanceUntilIdle()
            viewModel.onIntent(AddExpenseIntent.CategorySelected(2))
            viewModel.onIntent(AddExpenseIntent.AmountChanged("51"))
            viewModel.onIntent(AddExpenseIntent.Save)
            advanceUntilIdle()

            assertNull(repository.saved)
            assertEquals(R.string.error_insufficient_balance, viewModel.state.value.error)
        }

    @Test
    fun `editing on same account restores original amount for balance validation`() =
        runTest {
            val original = transaction(amount = "40", balance = "10")
            val repository = EditorRepository(accounts = listOf(original.account), transaction = original)
            val viewModel = viewModel(repository)
            viewModel.onIntent(AddExpenseIntent.Open(original.id, LocalDate.of(2026, 7, 22)))
            advanceUntilIdle()
            viewModel.onIntent(AddExpenseIntent.AmountChanged("45"))
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(AddExpenseIntent.Save)
            advanceUntilIdle()

            assertEquals(BigDecimal("45"), repository.saved?.amount)
            assertEquals(AddExpenseEffect.Saved, effect.await())
        }

    @Test
    fun `editor clamps default and changed dates to today`() =
        runTest {
            val repository = EditorRepository(accounts = listOf(account(balance = "50")))
            val viewModel = viewModel(repository)

            viewModel.onIntent(AddExpenseIntent.Open(null, LocalDate.of(2026, 7, 25)))
            advanceUntilIdle()
            assertEquals(LocalDate.of(2026, 7, 22), viewModel.state.value.date)

            viewModel.onIntent(AddExpenseIntent.DateChanged(LocalDate.of(2026, 8, 1)))
            assertEquals(LocalDate.of(2026, 7, 22), viewModel.state.value.date)
        }

    private fun viewModel(repository: FinanceRepository) =
        AddExpenseViewModel(
            GetEditorAccountsUseCase(repository),
            GetEditorCategoriesUseCase(repository),
            GetTransactionUseCase(repository),
            SaveTransactionUseCase(repository),
            clock,
        )
}

private class EditorRepository(
    private val accounts: List<Account>,
    private val transaction: Transaction? = null,
) : FinanceRepository {
    var saved: SaveTransactionCommand? = null

    override suspend fun getAccounts() = EditorResult.Success(accounts)

    override suspend fun getTransactions(query: TransactionsQuery): FinanceDataLoadResult<List<Transaction>> {
        TODO("Not yet implemented")
    }

    override suspend fun getCategories(isIncome: Boolean) =
        EditorResult.Success(listOf(Category(2, "Еда", "🍔", false)))

    override suspend fun getTransaction(id: Int) = EditorResult.Success(requireNotNull(transaction))

    override suspend fun saveTransaction(command: SaveTransactionCommand): EditorResult<Unit> {
        saved = command
        return EditorResult.Success(Unit)
    }

//    override suspend fun getDailySummary(
//        date: LocalDate,
//        currencyCode: String,
//        transactionKind: com.yandex.school.casheye.domain.finance.TransactionKind,
//    ) = error("Not used")
//
//    override suspend fun getAccountsSummary(currencyCode: String) = error("Not used")
//
//    override suspend fun getAnalytics(query: com.yandex.school.casheye.domain.finance.AnalyticsQuery) =
//        error("Not used")
}

private fun account(balance: String) = Account(1, "Основной", "💵", BigDecimal(balance), "RUB")

private fun transaction(
    amount: String,
    balance: String,
) = Transaction(
    id = 7,
    account = account(balance),
    category = Category(2, "Еда", "🍔", false),
    amount = BigDecimal(amount),
    transactionDate = Instant.parse("2026-07-22T08:00:00Z"),
    comment = null,
    createdAt = Instant.parse("2026-07-22T08:00:00Z"),
    updatedAt = Instant.parse("2026-07-22T08:00:00Z"),
)
