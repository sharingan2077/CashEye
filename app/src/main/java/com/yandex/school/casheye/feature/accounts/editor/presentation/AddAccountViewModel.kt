package com.yandex.school.casheye.feature.accounts.editor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.accounts.repository.AccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime


data class AddAccountUiState(
    val amount: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.now(),
    val selectedCurrency: String? = null,
    val availableCurrencies: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val canConfirm: Boolean
        get() = amount.toBigDecimalOrNull()?.signum() == 1 &&
                selectedCurrency != null
}

@HiltViewModel
class AddAccountViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    // Позже можно добавить:
    // private val categoriesRepository: CategoriesRepository,
    // private val AccountsRepository: AccountsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAccountUiState())
    val uiState: StateFlow<AddAccountUiState> = _uiState.asStateFlow()

    fun start() {
        _uiState.update { current ->
            AddAccountUiState(
                availableCurrencies = current.availableCurrencies,
            )
        }
    }

    fun onAction(action: AddAccountAction) {
        when (action) {
            is AddAccountAction.AmountChanged -> {
                val filteredValue = action.value.filter(Char::isDigit)

                _uiState.update {
                    it.copy(amount = filteredValue)
                }
            }

            is AddAccountAction.DateSelected -> {
                _uiState.update {
                    it.copy(selectedDate = action.date)
                }
            }

            is AddAccountAction.TimeSelected -> {
                _uiState.update {
                    it.copy(selectedTime = action.time)
                }
            }

            is AddAccountAction.CurrencySelected -> {
                _uiState.update {
                    it.copy(selectedCurrency = action.currency)
                }
            }

            AddAccountAction.ConfirmClicked -> saveAccount()
        }
    }

    private fun saveAccount() {
        val state = _uiState.value

        if (!state.canConfirm) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            // AccountsRepository.createAccount(...)

            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
