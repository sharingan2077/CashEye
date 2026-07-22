package com.yandex.school.casheye.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.EditorResult
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.GetAccountUseCase
import com.yandex.school.casheye.domain.finance.SaveAccountCommand
import com.yandex.school.casheye.domain.finance.SaveAccountUseCase
import com.yandex.school.casheye.feature.accounts.R
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

@Inject
class AccountEditorViewModel(
    private val getAccount: GetAccountUseCase,
    private val saveAccount: SaveAccountUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
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
            is AccountEditorIntent.CurrencyChanged -> _state.value = _state.value.copy(currency = intent.value)
            AccountEditorIntent.Save -> save()
        }
    }

    private fun load(id: Int?) {
        viewModelScope.launch {
            _state.value =
                AccountEditorUiState(
                    editingId = id,
                    openedDate = LocalDate.now(clock),
                    openedTime = LocalTime.now(clock).withSecond(0).withNano(0),
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
        if (normalized.count { it == '.' } <= 1 && normalized.all { it.isDigit() || it == '.' }) {
            _state.value = _state.value.copy(balance = normalized, error = null)
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
                state.currency !in SUPPORTED_CURRENCIES -> R.string.error_select_currency
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

private val SUPPORTED_CURRENCIES = setOf("RUB", "USD", "EUR", "GBP", "CNY")

private fun FinanceFailureReason.editorMessage(): Int =
    when (this) {
        FinanceFailureReason.Network -> R.string.error_network
        FinanceFailureReason.Authorization -> R.string.error_authorization
        FinanceFailureReason.Server -> R.string.error_server
        FinanceFailureReason.Unknown -> R.string.error_save_account
    }
