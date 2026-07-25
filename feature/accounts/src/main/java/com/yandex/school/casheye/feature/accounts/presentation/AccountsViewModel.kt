package com.yandex.school.casheye.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.AccountsLoadResult
import com.yandex.school.casheye.domain.finance.AccountsSummary
import com.yandex.school.casheye.domain.finance.DeleteAccountUseCase
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.GetAccountTransactionCountUseCase
import com.yandex.school.casheye.domain.finance.GetAccountsUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
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

    private var observeJob: Job? = null
    private var refreshJob: Job? = null
    private var latestSummary: AccountsSummary? = null
    private var initialRefreshCompleted = false
    private var localObservationReady = CompletableDeferred<Unit>()

    init {
        observeAccounts()
        refreshAccounts()
    }

    fun onIntent(intent: AccountsIntent) {
        when (intent) {
            AccountsIntent.Retry -> refreshAccounts()
            AccountsIntent.Refresh -> refreshAccounts()
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

    private fun observeAccounts() {
        observeJob?.cancel()
        val observationReady = localObservationReady
        observeJob =
            viewModelScope.launch {
                getAccounts(currencyCode = CURRENCY_RUB).collectLatest { result ->
                    observationReady.complete(Unit)
                    when (result) {
                        is AccountsLoadResult.Success -> {
                            latestSummary = result.summary
                            renderSummary()
                        }

                        is AccountsLoadResult.Failure -> {
                            if (!_state.value.isRefreshable()) {
                                _state.value = AccountsUiState.Error(result.reason)
                            }
                        }
                    }
                }
            }
    }

    private fun refreshAccounts() {
        refreshJob?.cancel()
        val observationReady = localObservationReady
        if (_state.value.isRefreshable()) {
            _state.value = _state.value.withRefreshing(true)
        }
        refreshJob =
            viewModelScope.launch {
                when (val result = getAccounts.refresh()) {
                    FinanceRefreshResult.Success -> {
                        initialRefreshCompleted = true
                        renderSummary(isRefreshing = false)
                    }

                    is FinanceRefreshResult.Failure -> {
                        observationReady.await()
                        initialRefreshCompleted = true
                        val hasVisibleCache =
                            _state.value.isRefreshable() || latestSummary?.accounts?.isNotEmpty() == true
                        if (hasVisibleCache) {
                            renderSummary(isRefreshing = false)
                            _effects.emit(AccountsEffect.ShowError(result.reason))
                        } else {
                            _state.value = AccountsUiState.Error(result.reason)
                        }
                    }
                }
            }
    }

    private fun renderSummary(isRefreshing: Boolean = refreshJob?.isActive == true) {
        val summary = latestSummary ?: return
        if (summary.accounts.isEmpty() && !initialRefreshCompleted) return
        val deleteConfirmation =
            (_state.value as? AccountsUiState.Content)
                ?.deleteConfirmation
                ?.takeIf { confirmation -> summary.accounts.any { it.id == confirmation.accountId } }
        _state.value =
            if (summary.accounts.isEmpty()) {
                AccountsUiState.Empty(isRefreshing)
            } else {
                AccountsUiState.Content(
                    total = summary.total,
                    currencyCode = summary.currencyCode,
                    accounts = summary.accounts,
                    deleteConfirmation = deleteConfirmation,
                    isRefreshing = isRefreshing,
                )
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
