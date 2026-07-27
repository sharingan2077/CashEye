package com.yandex.school.casheye.feature.accounts.presentation.efitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.GetAccountUseCase
import com.yandex.school.casheye.domain.finance.GetAccountCurrencyChangeEligibilityUseCase
import com.yandex.school.casheye.domain.finance.SaveAccountUseCase
import com.yandex.school.casheye.domain.finance.editor.AccountCurrencyChangeEligibility
import com.yandex.school.casheye.domain.finance.editor.EditorResult
import com.yandex.school.casheye.domain.finance.editor.SaveAccountCommand
import com.yandex.school.casheye.feature.accounts.R
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class AccountEditorViewModel(
    private val getAccount: GetAccountUseCase,
    private val getAccountCurrencyChangeEligibility: GetAccountCurrencyChangeEligibilityUseCase,
    private val saveAccount: SaveAccountUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountEditorUiState())
    val state = _state.asStateFlow()
    private val _effects = MutableSharedFlow<AccountEditorEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: AccountEditorIntent) {
        when (intent) {
            is AccountEditorIntent.Open -> load(intent.accountId)
            is AccountEditorIntent.NameChanged -> _state.value = _state.value.copy(name = intent.value, error = null)
            is AccountEditorIntent.BalanceChanged -> updateBalance(intent.value)
            is AccountEditorIntent.CurrencyChanged -> _state.value = _state.value.copy(currency = intent.value, error = null)
            AccountEditorIntent.CurrencyChangeRequested -> requestCurrencyChange()
            is AccountEditorIntent.EmojiChanged -> _state.value = _state.value.copy(emoji = intent.value)
            AccountEditorIntent.Save -> save()
        }
    }

    private fun load(id: Int?) {
        viewModelScope.launch {
            _state.value =
                AccountEditorUiState(
                    editingId = id,
                )
            if (id == null) {
                _state.value = _state.value.copy(isLoading = false)
                return@launch
            }
            when (val result = getAccount(id)) {
                is EditorResult.Success -> {
                    _state.value =
                        _state.value.copy(
                            isLoading = false,
                            name = result.value.name,
                            balance = result.value.balance.toPlainString(),
                            currency = result.value.currency,
                            emoji = result.value.emoji,
                        )
                }

                is EditorResult.Failure -> {
                    _state.value =
                        _state.value.copy(isLoading = false, error = result.reason.editorMessage())
                }
            }
        }
    }

    private fun updateBalance(value: String) {
        val normalized = value.replace(',', '.')
        if (
            normalized.count { it == '.' } <= 1 &&
            normalized.all { it.isDigit() || it == '.' } &&
            normalized.substringAfter('.', "").length <= 2
        ) {
            _state.value = _state.value.copy(balance = normalized, error = null)
        }
    }

    private fun requestCurrencyChange() {
        val accountId = _state.value.editingId ?: return
        if (_state.value.isCheckingCurrency) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isCheckingCurrency = true, error = null)
            when (val result = getAccountCurrencyChangeEligibility(accountId)) {
                is EditorResult.Success -> {
                    when (result.value) {
                        AccountCurrencyChangeEligibility.Allowed -> _effects.emit(AccountEditorEffect.OpenCurrencySelector)
                        AccountCurrencyChangeEligibility.HasTransactions -> {
                            _state.value = _state.value.copy(error = R.string.error_account_currency_has_transactions)
                        }

                        AccountCurrencyChangeEligibility.HistoryUnavailable -> {
                            _state.value = _state.value.copy(error = R.string.error_account_currency_history_unavailable)
                        }
                    }
                }

                is EditorResult.Failure -> {
                    _state.value = _state.value.copy(error = result.reason.editorMessage())
                }
            }
            _state.value = _state.value.copy(isCheckingCurrency = false)
        }
    }

    private fun save() {
        val state = _state.value
        if (state.isSaving) return
        val balance = state.balance.toBigDecimalOrNull()
        val validation =
            when {
                state.name.isBlank() -> R.string.error_account_name_required
                balance == null || balance.signum() < 0 -> R.string.error_invalid_balance
                else -> null
            }
        if (validation != null) {
            _state.value = state.copy(error = validation)
            return
        }
        viewModelScope.launch {
            _state.value = state.copy(isSaving = true, error = null)
            when (
                val result =
                    saveAccount(
                        SaveAccountCommand(
                            id = state.editingId,
                            name = state.name.trim(),
                            emoji = state.emoji,
                            balance = requireNotNull(balance),
                            currency = state.currency,
                        ),
                    )
            ) {
                is EditorResult.Success -> {
                    _effects.emit(AccountEditorEffect.Saved)
                }

                is EditorResult.Failure -> {
                    _state.value =
                        _state.value.copy(isSaving = false, error = result.reason.editorMessage())
                }
            }
        }
    }
}

private fun FinanceFailureReason.editorMessage(): Int =
    when (this) {
        FinanceFailureReason.Network -> R.string.error_network
        FinanceFailureReason.Authorization -> R.string.error_authorization
        FinanceFailureReason.Server -> R.string.error_server
        FinanceFailureReason.Unknown -> R.string.error_save_account
    }
