package com.yandex.school.casheye.feature.accounts.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Transaction
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.AccountsSummary
import com.yandex.school.casheye.domain.finance.FinanceDataLoadResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAccountsUseCase
import com.yandex.school.casheye.domain.finance.TransactionsQuery
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

            assertEquals(AccountsUiState.Empty(), viewModel.state.value)
        }

    @Test
    fun `initial failure exposes error and retry loads accounts again`() =
        runTest {
            val repository =
                FakeAccountsRepository(
                    AccountsLoadResult.Failure(FinanceFailureReason.Network),
                    AccountsLoadResult.Success(AccountsSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = AccountsViewModel(GetAccountsUseCase(repository))

            advanceUntilIdle()

            assertTrue(viewModel.state.value is AccountsUiState.Error)

            viewModel.onIntent(AccountsIntent.Retry)
            advanceUntilIdle()

            assertEquals(2, repository.requestedCurrencies.size)
            assertEquals(AccountsUiState.Empty(), viewModel.state.value)
        }

    @Test
    fun `failed refresh keeps content and emits show error effect`() =
        runTest {
            val summary = AccountsSummary(BigDecimal("125000.00"), "RUB", listOf(account()))
            val viewModel =
                AccountsViewModel(
                    GetAccountsUseCase(
                        FakeAccountsRepository(
                            AccountsLoadResult.Success(summary),
                            AccountsLoadResult.Failure(FinanceFailureReason.Network),
                        ),
                    ),
                )

            advanceUntilIdle()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(AccountsIntent.Refresh)
            advanceUntilIdle()

            assertEquals(
                AccountsUiState.Content(summary.total, summary.currencyCode, summary.accounts),
                viewModel.state.value,
            )
            assertEquals(AccountsEffect.ShowError(FinanceFailureReason.Network), effect.await())
        }

    @Test
    fun `refresh reloads accounts`() =
        runTest {
            val repository =
                FakeAccountsRepository(
                    AccountsLoadResult.Success(AccountsSummary(BigDecimal.ZERO, "RUB", emptyList())),
                    AccountsLoadResult.Success(AccountsSummary(BigDecimal.ZERO, "RUB", emptyList())),
                )
            val viewModel = AccountsViewModel(GetAccountsUseCase(repository))

            advanceUntilIdle()
            viewModel.onIntent(AccountsIntent.Refresh)
            advanceUntilIdle()

            assertEquals(listOf("RUB", "RUB"), repository.requestedCurrencies)
        }
}

private class FakeAccountsRepository(
    vararg results: AccountsLoadResult,
) : FinanceRepository {
    private val results = ArrayDeque(results.toList())
    val requestedCurrencies = mutableListOf<String>()

    override suspend fun getAccounts(): FinanceDataLoadResult<List<Account>> {
        requestedCurrencies += "RUB"
        return when (val result = results.removeFirst()) {
            is AccountsLoadResult.Success -> FinanceDataLoadResult.Success(result.summary.accounts)
            is AccountsLoadResult.Failure -> FinanceDataLoadResult.Failure(result.reason)
        }
    }

    override suspend fun getTransactions(query: TransactionsQuery): FinanceDataLoadResult<List<Transaction>> =
        error("Transactions are not requested by AccountsViewModel")
}

private fun account(): Account =
    Account(
        id = 1,
        name = "Основной счёт",
        emoji = "💳",
        balance = BigDecimal("125000.00"),
        currency = "RUB",
    )
