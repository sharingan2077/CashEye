package com.yandex.school.casheye.feature.accounts.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.domain.finance.AccountsCurrentValuation
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.AccountsSummary
import com.yandex.school.casheye.domain.finance.DeleteAccountUseCase
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAccountTransactionCountUseCase
import com.yandex.school.casheye.domain.finance.GetAccountsUseCase
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
            val summary =
                AccountsSummary(
                    nativeTotals = listOf(MoneyAmount(BigDecimal("125000.00"), CurrencyCode.RUB)),
                    currentValuation = currentValuation(),
                    accounts = listOf(account()),
                )
            val repository = FakeAccountsRepository(AccountsLoadResult.Success(summary))
            val viewModel = accountsViewModel(repository)

            advanceUntilIdle()

            assertEquals(
                AccountsUiState.Content(summary.nativeTotals, summary.currentValuation, summary.accounts),
                viewModel.state.value,
            )
            assertEquals(listOf("RUB"), repository.requestedCurrencies)
        }

    @Test
    fun `empty load exposes empty state`() =
        runTest {
            val viewModel =
                accountsViewModel(
                    FakeAccountsRepository(
                        AccountsLoadResult.Success(AccountsSummary(emptyList(), null, emptyList())),
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
                    AccountsLoadResult.Success(AccountsSummary(emptyList(), null, emptyList())),
                )
            val viewModel = accountsViewModel(repository)

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
            val summary =
                AccountsSummary(
                    nativeTotals = listOf(MoneyAmount(BigDecimal("125000.00"), CurrencyCode.RUB)),
                    currentValuation = currentValuation(),
                    accounts = listOf(account()),
                )
            val viewModel =
                accountsViewModel(
                    FakeAccountsRepository(
                        AccountsLoadResult.Success(summary),
                        AccountsLoadResult.Failure(FinanceFailureReason.Server),
                    ),
                )

            advanceUntilIdle()
            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(AccountsIntent.Refresh)
            advanceUntilIdle()

            assertEquals(
                AccountsUiState.Content(summary.nativeTotals, summary.currentValuation, summary.accounts),
                viewModel.state.value,
            )
            assertEquals(AccountsEffect.ShowError(FinanceFailureReason.Server), effect.await())
        }

    @Test
    fun `refresh reloads accounts`() =
        runTest {
            val repository =
                FakeAccountsRepository(
                    AccountsLoadResult.Success(AccountsSummary(emptyList(), null, emptyList())),
                    AccountsLoadResult.Success(AccountsSummary(emptyList(), null, emptyList())),
                )
            val viewModel = accountsViewModel(repository)

            advanceUntilIdle()
            viewModel.onIntent(AccountsIntent.Refresh)
            advanceUntilIdle()

            assertEquals(listOf("RUB", "RUB"), repository.requestedCurrencies)
        }

    @Test
    fun `cached accounts stay visible while initial refresh is running`() =
        runTest {
            val refresh = CompletableDeferred<FinanceRefreshResult>()
            val repository =
                object : StubFinanceRepository() {
                    override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(listOf(account()))

                    override suspend fun refreshAccounts(): FinanceRefreshResult = refresh.await()
                }
            val viewModel = accountsViewModel(repository)

            runCurrent()

            val state = viewModel.state.value as AccountsUiState.Content
            assertTrue(state.isRefreshing)
            assertEquals(listOf(1), state.accounts.map { it.id })

            refresh.complete(FinanceRefreshResult.Success)
            advanceUntilIdle()
            assertEquals(false, (viewModel.state.value as AccountsUiState.Content).isRefreshing)
        }

    @Test
    fun `network failure waits for cached accounts without emitting error`() =
        runTest {
            val cacheReady = CompletableDeferred<Unit>()
            val repository =
                object : StubFinanceRepository() {
                    override fun observeAccounts(): Flow<List<Account>> =
                        flow {
                            cacheReady.await()
                            emit(listOf(account()))
                        }

                    override suspend fun refreshAccounts(): FinanceRefreshResult =
                        FinanceRefreshResult.Failure(
                            FinanceFailureReason.Network,
                            hasUsableCache = true,
                        )
                }
            val viewModel = accountsViewModel(repository)

            runCurrent()
            assertEquals(AccountsUiState.Loading, viewModel.state.value)

            val effects = mutableListOf<AccountsEffect>()
            val collector =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.effects.collect { effects += it }
                }
            cacheReady.complete(Unit)
            advanceUntilIdle()

            assertTrue(viewModel.state.value is AccountsUiState.Content)
            assertTrue(effects.isEmpty())
            collector.cancel()
        }

    @Test
    fun `offline refresh exposes empty accounts when cache is initialized`() =
        runTest {
            val repository =
                object : StubFinanceRepository() {
                    override fun observeAccounts(): Flow<List<Account>> = MutableStateFlow(emptyList())

                    override suspend fun refreshAccounts(): FinanceRefreshResult =
                        FinanceRefreshResult.Failure(
                            FinanceFailureReason.Network,
                            hasUsableCache = true,
                        )
                }

            val viewModel = accountsViewModel(repository)
            val effects = mutableListOf<AccountsEffect>()
            val collector =
                backgroundScope.async(start = CoroutineStart.UNDISPATCHED) {
                    viewModel.effects.collect { effects += it }
                }
            advanceUntilIdle()

            assertEquals(AccountsUiState.Empty(), viewModel.state.value)
            assertTrue(effects.isEmpty())
            collector.cancel()
        }

    @Test
    fun `account with transactions requires confirmation before cascade delete`() =
        runTest {
            val summary =
                AccountsSummary(
                    nativeTotals = listOf(MoneyAmount(BigDecimal("125000.00"), CurrencyCode.RUB)),
                    currentValuation = null,
                    accounts = listOf(account()),
                )
            val repository = FakeAccountsRepository(AccountsLoadResult.Success(summary))
            repository.transactionCount = 3
            val viewModel = accountsViewModel(repository)

            advanceUntilIdle()
            viewModel.onIntent(AccountsIntent.RequestAccountDelete(1))
            advanceUntilIdle()

            assertEquals(
                AccountDeleteConfirmation(accountId = 1, transactionCount = 3),
                (viewModel.state.value as AccountsUiState.Content).deleteConfirmation,
            )
            assertTrue(repository.deletedAccountIds.isEmpty())

            val effect = async(start = CoroutineStart.UNDISPATCHED) { viewModel.effects.first() }
            viewModel.onIntent(AccountsIntent.ConfirmAccountDelete)
            advanceUntilIdle()

            assertEquals(listOf(1), repository.deletedAccountIds)
            assertEquals(AccountsUiState.Empty(), viewModel.state.value)
            assertEquals(AccountsEffect.AccountDeleted(3), effect.await())
        }
}

private fun accountsViewModel(repository: FinanceRepository): AccountsViewModel =
    AccountsViewModel(
        GetAccountsUseCase(repository),
        GetAccountTransactionCountUseCase(repository),
        DeleteAccountUseCase(repository),
    )

private open class StubFinanceRepository : FinanceRepository {
    override suspend fun getAccounts() = error("Not used")

    override suspend fun getTransactions(query: TransactionsQuery) = error("Not used")
}

private class FakeAccountsRepository(
    vararg results: AccountsLoadResult,
) : StubFinanceRepository() {
    private val results = ArrayDeque(results.toList())
    private val accounts =
        MutableStateFlow(
            (results.firstOrNull() as? AccountsLoadResult.Success)?.summary?.accounts.orEmpty(),
        )
    val requestedCurrencies = mutableListOf<String>()
    val deletedAccountIds = mutableListOf<Int>()
    var transactionCount: Int = 0

    override fun observeAccounts(): Flow<List<Account>> = accounts

    override suspend fun refreshAccounts(): FinanceRefreshResult {
        requestedCurrencies += "RUB"
        return when (val result = results.removeFirst()) {
            is AccountsLoadResult.Success -> {
                accounts.value = result.summary.accounts
                FinanceRefreshResult.Success
            }

            is AccountsLoadResult.Failure -> {
                FinanceRefreshResult.Failure(result.reason, hasUsableCache = false)
            }
        }
    }

    override suspend fun getAccountTransactionCount(id: Int): EditorResult<Int> = EditorResult.Success(transactionCount)

    override suspend fun deleteAccount(id: Int): EditorResult<Int> {
        deletedAccountIds += id
        accounts.value = accounts.value.filterNot { it.id == id }
        return EditorResult.Success(transactionCount)
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

private fun currentValuation(): AccountsCurrentValuation =
    AccountsCurrentValuation(
        includedTotal = MoneyAmount(BigDecimal("125000.00"), CurrencyCode.RUB),
        excludedNativeTotals = emptyList(),
        rateDate = null,
    )
