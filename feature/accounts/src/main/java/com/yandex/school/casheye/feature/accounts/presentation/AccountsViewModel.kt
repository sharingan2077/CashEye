package com.yandex.school.casheye.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.GetAccountsUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class AccountsViewModel(
    private val getAccounts: GetAccountsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow<AccountsUiState>(AccountsUiState.Loading)
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AccountsEffect>()
    val effects: SharedFlow<AccountsEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    init {
        loadAccounts()
    }

    fun onIntent(intent: AccountsIntent) {
        when (intent) {
            AccountsIntent.Retry -> loadAccounts()
        }
    }

    private fun loadAccounts() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _state.value = AccountsUiState.Loading
                when (
                    val result =
                        getAccounts(currencyCode = CURRENCY_RUB)
                ) {
                    is AccountsLoadResult.Success -> {
                        val summary = result.summary
                        _state.value =
                            if (summary.accounts.isEmpty()) {
                                AccountsUiState.Empty
                            } else {
                                AccountsUiState.Content(
                                    total = summary.total,
                                    currencyCode = summary.currencyCode,
                                    accounts = summary.accounts,
                                )
                            }
                    }

                    is AccountsLoadResult.Failure -> {
                        _state.value = AccountsUiState.Error(result.reason)
                        _effects.emit(AccountsEffect.ShowError(result.reason))
                    }
                }
            }
    }
}

private const val CURRENCY_RUB = "RUB"
