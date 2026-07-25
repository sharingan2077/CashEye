package com.yandex.school.casheye.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.DeleteAccountUseCase
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.GetAccountTransactionCountUseCase
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
    private val getAccountTransactionCount: GetAccountTransactionCountUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
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
            AccountsIntent.Retry -> loadAccounts(preserveContent = _state.value.isRefreshable())
            AccountsIntent.Refresh -> loadAccounts(preserveContent = true)
            is AccountsIntent.RequestAccountDelete -> requestAccountDelete(intent.id)
            AccountsIntent.ConfirmAccountDelete -> confirmAccountDelete()
            AccountsIntent.CancelAccountDelete -> updateDeleteConfirmation(null)
        }
    }

    private fun requestAccountDelete(id: Int) {
        viewModelScope.launch {
            when (val result = getAccountTransactionCount(id)) {
                is EditorResult.Success -> {
                    if (result.value == 0) {
                        deleteAccount(id)
                    } else {
                        updateDeleteConfirmation(AccountDeleteConfirmation(id, result.value))
                    }
                }

                is EditorResult.Failure -> _effects.emit(AccountsEffect.ShowDeleteError(result.reason))
            }
        }
    }

    private fun confirmAccountDelete() {
        val confirmation =
            (_state.value as? AccountsUiState.Content)?.deleteConfirmation
                ?: return
        updateDeleteConfirmation(null)
        viewModelScope.launch { deleteAccount(confirmation.accountId) }
    }

    private suspend fun deleteAccount(id: Int) {
        when (val result = deleteAccountUseCase(id)) {
            is EditorResult.Success -> {
                removeAccount(id)
                _effects.emit(AccountsEffect.AccountDeleted(result.value))
            }

            is EditorResult.Failure -> _effects.emit(AccountsEffect.ShowDeleteError(result.reason))
        }
    }

    private fun updateDeleteConfirmation(confirmation: AccountDeleteConfirmation?) {
        val content = _state.value as? AccountsUiState.Content ?: return
        _state.value = content.copy(deleteConfirmation = confirmation)
    }

    private fun removeAccount(id: Int) {
        val content = _state.value as? AccountsUiState.Content ?: return
        val removed = content.accounts.firstOrNull { it.id == id } ?: return
        val remaining = content.accounts.filterNot { it.id == id }
        _state.value =
            if (remaining.isEmpty()) {
                AccountsUiState.Empty()
            } else {
                content.copy(
                    total = content.total.subtract(removed.balance),
                    accounts = remaining,
                    deleteConfirmation = null,
                    isRefreshing = false,
                )
            }
    }

    private fun loadAccounts(preserveContent: Boolean = false) {
        if (loadJob?.isActive == true) return
        val keepsVisibleContent = preserveContent && _state.value.isRefreshable()
        loadJob =
            viewModelScope.launch {
                _state.value =
                    if (keepsVisibleContent) {
                        _state.value.withRefreshing(true)
                    } else {
                        AccountsUiState.Loading
                    }
                when (
                    val result =
                        getAccounts(currencyCode = CURRENCY_RUB)
                ) {
                    is AccountsLoadResult.Success -> {
                        val summary = result.summary
                        _state.value =
                            if (summary.accounts.isEmpty()) {
                                AccountsUiState.Empty()
                            } else {
                                AccountsUiState.Content(
                                    total = summary.total,
                                    currencyCode = summary.currencyCode,
                                    accounts = summary.accounts,
                                )
                            }
                    }

                    is AccountsLoadResult.Failure -> {
                        _state.value =
                            if (keepsVisibleContent) {
                                _state.value.withRefreshing(false)
                            } else {
                                AccountsUiState.Error(result.reason)
                            }
                        if (keepsVisibleContent) {
                            _effects.emit(AccountsEffect.ShowError(result.reason))
                        }
                    }
                }
            }
    }
}

private fun AccountsUiState.isRefreshable(): Boolean = this is AccountsUiState.Content || this is AccountsUiState.Empty

private fun AccountsUiState.withRefreshing(isRefreshing: Boolean): AccountsUiState =
    when (this) {
        is AccountsUiState.Content -> copy(isRefreshing = isRefreshing)

        is AccountsUiState.Empty -> copy(isRefreshing = isRefreshing)

        AccountsUiState.Loading,
        is AccountsUiState.Error,
        -> this
    }

private const val CURRENCY_RUB = "RUB"
