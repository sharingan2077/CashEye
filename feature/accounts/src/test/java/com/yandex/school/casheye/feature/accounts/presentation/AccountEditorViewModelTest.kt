package com.yandex.school.casheye.feature.accounts.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.core.model.FinanceEditorInputLimits
import com.yandex.school.casheye.domain.finance.FinanceRepository
import com.yandex.school.casheye.domain.finance.GetAccountCurrencyChangeEligibilityUseCase
import com.yandex.school.casheye.domain.finance.GetAccountUseCase
import com.yandex.school.casheye.domain.finance.SaveAccountUseCase
import com.yandex.school.casheye.domain.finance.TransactionsQuery
import com.yandex.school.casheye.domain.finance.editor.AccountCurrencyChangeEligibility
import com.yandex.school.casheye.domain.finance.editor.EditorResult
import com.yandex.school.casheye.domain.finance.editor.SaveAccountCommand
import com.yandex.school.casheye.feature.accounts.R
import com.yandex.school.casheye.feature.accounts.presentation.efitor.AccountEditorIntent
import com.yandex.school.casheye.feature.accounts.presentation.efitor.AccountEditorViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class AccountEditorViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `editing loads account and saves full update command`() =
        runTest {
            val repository = AccountEditorRepository()
            val viewModel =
                AccountEditorViewModel(
                    GetAccountUseCase(repository),
                    GetAccountCurrencyChangeEligibilityUseCase(repository),
                    SaveAccountUseCase(repository),
                )
            viewModel.onIntent(AccountEditorIntent.Open(5))
            advanceUntilIdle()
            viewModel.onIntent(AccountEditorIntent.NameChanged(" \nРезерв\n "))
            assertEquals("Резерв", viewModel.state.value.name)
            viewModel.onIntent(AccountEditorIntent.CurrencyChanged(CurrencyCode.USD))
            viewModel.onIntent(AccountEditorIntent.EmojiChanged("🏦"))
            viewModel.onIntent(AccountEditorIntent.Save)
            advanceUntilIdle()

            assertEquals(SaveAccountCommand(5, "Резерв", "🏦", BigDecimal("100.00"), "USD"), repository.saved)
        }

    @Test
    fun `creating saves default emoji`() =
        runTest {
            val repository = AccountEditorRepository()
            val viewModel =
                AccountEditorViewModel(
                    GetAccountUseCase(repository),
                    GetAccountCurrencyChangeEligibilityUseCase(repository),
                    SaveAccountUseCase(repository),
                )
            viewModel.onIntent(AccountEditorIntent.Open(null))
            advanceUntilIdle()
            viewModel.onIntent(AccountEditorIntent.NameChanged(" \nОсновной\n "))
            assertEquals("Основной", viewModel.state.value.name)
            viewModel.onIntent(AccountEditorIntent.BalanceChanged("100.00"))
            viewModel.onIntent(AccountEditorIntent.Save)
            advanceUntilIdle()

            assertEquals(SaveAccountCommand(null, "Основной", "💵", BigDecimal("100.00"), "RUB"), repository.saved)
        }

    @Test
    fun `account name is limited to fifty characters`() =
        runTest {
            val repository = AccountEditorRepository()
            val viewModel =
                AccountEditorViewModel(
                    GetAccountUseCase(repository),
                    GetAccountCurrencyChangeEligibilityUseCase(repository),
                    SaveAccountUseCase(repository),
                )

            viewModel.onIntent(AccountEditorIntent.NameChanged("a".repeat(51)))

            assertEquals(FinanceEditorInputLimits.ACCOUNT_NAME_MAX_LENGTH, viewModel.state.value.name.length)
        }

    @Test
    fun `currency selection reports an error when account has transactions`() =
        runTest {
            val repository =
                AccountEditorRepository().apply {
                    currencyEligibility = AccountCurrencyChangeEligibility.HasTransactions
                }
            val viewModel =
                AccountEditorViewModel(
                    GetAccountUseCase(repository),
                    GetAccountCurrencyChangeEligibilityUseCase(repository),
                    SaveAccountUseCase(repository),
                )
            viewModel.onIntent(AccountEditorIntent.Open(5))
            advanceUntilIdle()
            viewModel.onIntent(AccountEditorIntent.CurrencyChangeRequested)
            advanceUntilIdle()

            assertEquals(R.string.error_account_currency_has_transactions, viewModel.state.value.error)
            assertEquals(CurrencyCode.RUB, viewModel.state.value.currency)
        }

    @Test
    fun `currency selection reports an error when history cannot be verified`() =
        runTest {
            val repository =
                AccountEditorRepository().apply {
                    currencyEligibility = AccountCurrencyChangeEligibility.HistoryUnavailable
                }
            val viewModel =
                AccountEditorViewModel(
                    GetAccountUseCase(repository),
                    GetAccountCurrencyChangeEligibilityUseCase(repository),
                    SaveAccountUseCase(repository),
                )
            viewModel.onIntent(AccountEditorIntent.Open(5))
            advanceUntilIdle()
            viewModel.onIntent(AccountEditorIntent.CurrencyChangeRequested)
            advanceUntilIdle()

            assertEquals(R.string.error_account_currency_history_unavailable, viewModel.state.value.error)
            assertEquals(CurrencyCode.RUB, viewModel.state.value.currency)
        }
}

private class AccountEditorRepository : FinanceRepository {
    var saved: SaveAccountCommand? = null
    var currencyEligibility: AccountCurrencyChangeEligibility = AccountCurrencyChangeEligibility.Allowed

    override suspend fun getAccounts() = error("Not used")

    override suspend fun getTransactions(query: TransactionsQuery) = error("Not used")

    override suspend fun getAccount(id: Int) =
        EditorResult.Success(Account(id, "Основной", "💳", BigDecimal("100.00"), "RUB"))

    override suspend fun saveAccount(command: SaveAccountCommand): EditorResult<Unit> {
        saved = command
        return EditorResult.Success(Unit)
    }

    override suspend fun getAccountCurrencyChangeEligibility(id: Int) = EditorResult.Success(currencyEligibility)
}
