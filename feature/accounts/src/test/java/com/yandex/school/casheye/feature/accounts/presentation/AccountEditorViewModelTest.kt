package com.yandex.school.casheye.feature.accounts.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.domain.finance.AnalyticsQuery
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAccountUseCase
import com.yandex.school.casheye.domain.finance.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.SaveAccountUseCase
import com.yandex.school.casheye.domain.finance.TransactionKind
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
class AccountEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)

    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `editing loads account and saves full update command`() =
        runTest {
            val repository = AccountEditorRepository()
            val viewModel =
                AccountEditorViewModel(
                    GetAccountUseCase(repository),
                    SaveAccountUseCase(repository),
                    Clock.fixed(Instant.parse("2026-07-22T10:00:00Z"), ZoneOffset.UTC),
                )
            viewModel.onIntent(AccountEditorIntent.Open(5))
            advanceUntilIdle()
            viewModel.onIntent(AccountEditorIntent.NameChanged("Резерв"))
            viewModel.onIntent(AccountEditorIntent.CurrencyChanged("USD"))
            viewModel.onIntent(AccountEditorIntent.Save)
            advanceUntilIdle()

            assertEquals(SaveAccountCommand(5, "Резерв", "💳", BigDecimal("100.00"), "USD"), repository.saved)
        }
}

private class AccountEditorRepository : FinanceRepository {
    var saved: SaveAccountCommand? = null

    override suspend fun getAccount(id: Int) =
        EditorResult.Success(Account(id, "Основной", "💳", BigDecimal("100.00"), "RUB"))

    override suspend fun saveAccount(command: SaveAccountCommand): EditorResult<Unit> {
        saved = command
        return EditorResult.Success(Unit)
    }

    override suspend fun getDailySummary(
        date: LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ) = error("Not used")

    override suspend fun getAccountsSummary(currencyCode: String) = error("Not used")

    override suspend fun getAnalytics(query: AnalyticsQuery) = error("Not used")
}
