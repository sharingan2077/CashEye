package com.yandex.school.casheye.feature.accounts.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.AccountsSummary
import com.yandex.school.casheye.domain.finance.AnalyticsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsQuery
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAccountsUseCase
import com.yandex.school.casheye.domain.finance.TransactionKind
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
import java.util.ArrayDeque

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `successful load exposes account content`() =
        runTest {
            val summary = AccountsSummary(BigDecimal("125000.00"), "RUB", listOf(account()))
            val repository = FakeAccountsRepository(AccountsLoadResult.Success(summary))
            val viewModel = AccountsViewModel(GetAccountsUseCase(repository))

            advanceUntilIdle()

            assertEquals(
                AccountsUiState.Content(summary.total, summary.currencyCode, summary.accounts),
                viewModel.state.value,
            )
            assertEquals(listOf("RUB"), repository.requestedCurrencies)
        }

    @Test
    fun `empty load exposes empty state`() =
        runTest {
            val viewModel =
                AccountsViewModel(
                    GetAccountsUseCase(
                        FakeAccountsRepository(
                            AccountsLoadResult.Success(AccountsSummary(BigDecimal.ZERO, "RUB", emptyList())),
                        ),
                    ),
                )

            advanceUntilIdle()

            assertEquals(AccountsUiState.Empty, viewModel.state.value)
        }

    @Test
    fun `failure exposes error and retry loads accounts again`() =
        runTest {
            val repository =
                FakeAccountsRepository(
                    AccountsLoadResult.Failure(FinanceFailureReason.Network),
                    AccountsLoadResult.Success(AccountsSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = AccountsViewModel(GetAccountsUseCase(repository))
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }

            advanceUntilIdle()

            assertTrue(viewModel.state.value is AccountsUiState.Error)
            assertEquals(AccountsEffect.ShowError("Проверьте подключение к интернету"), effect.await())

            viewModel.onIntent(AccountsIntent.Retry)
            advanceUntilIdle()

            assertEquals(2, repository.requestedCurrencies.size)
            assertEquals(AccountsUiState.Empty, viewModel.state.value)
        }
}

private class FakeAccountsRepository(
    vararg results: AccountsLoadResult,
) : FinanceRepository {
    private val results = ArrayDeque(results.toList())
    val requestedCurrencies = mutableListOf<String>()

    override suspend fun getDailySummary(
        date: java.time.LocalDate,
        currencyCode: String,
        transactionKind: TransactionKind,
    ): FinanceLoadResult = error("Daily summary is not requested by AccountsViewModel")

    override suspend fun getAccountsSummary(currencyCode: String): AccountsLoadResult {
        requestedCurrencies += currencyCode
        return results.removeFirst()
    }

    override suspend fun getAnalytics(query: AnalyticsQuery): AnalyticsLoadResult {
        TODO("Not yet implemented")
    }
}

private fun account(): Account =
    Account(
        id = 1,
        name = "Основной счёт",
        emoji = "💳",
        balance = BigDecimal("125000.00"),
        currency = "RUB",
    )
