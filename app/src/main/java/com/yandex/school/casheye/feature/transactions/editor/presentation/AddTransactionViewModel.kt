package com.yandex.school.casheye.feature.transactions.editor.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.domain.accounts.repository.AccountsRepository
import com.yandex.school.casheye.domain.categories.repository.CategoriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime


data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val selectedCategory: Category? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedTime: LocalTime = LocalTime.now(),
    val selectedAccount: Account? = null,
    val availableCategories: List<Category> = emptyList(),
    val availableAccounts: List<Account> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null,
) {
    val canConfirm: Boolean
        get() = amount.toBigDecimalOrNull()?.signum() == 1 &&
                selectedCategory != null &&
                selectedAccount != null
}

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val accountsRepository: AccountsRepository,
    private val categoriesRepository: CategoriesRepository
    // Позже можно добавить:
    // private val transactionsRepository: TransactionsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    fun start(type: TransactionType) {
        _uiState.update { current ->
            AddTransactionUiState(
                type = type,
                availableAccounts = current.availableAccounts,
                availableCategories = current.availableCategories,
            )
        }

        loadAccounts()
        loadCategories(type)
    }

    fun onAction(action: AddTransactionAction) {
        when (action) {
            is AddTransactionAction.AmountChanged -> {
                val filteredValue = action.value.filter(Char::isDigit)

                _uiState.update {
                    it.copy(amount = filteredValue)
                }
            }

            is AddTransactionAction.CategorySelected -> {
                _uiState.update {
                    it.copy(selectedCategory = action.category)
                }
            }

            is AddTransactionAction.DateSelected -> {
                _uiState.update {
                    it.copy(selectedDate = action.date)
                }
            }

            is AddTransactionAction.TimeSelected -> {
                _uiState.update {
                    it.copy(selectedTime = action.time)
                }
            }

            is AddTransactionAction.AccountSelected -> {
                _uiState.update {
                    it.copy(selectedAccount = action.account)
                }
            }

            AddTransactionAction.ConfirmClicked -> saveTransaction()
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountsRepository.observeAccounts().collect { accounts ->
                _uiState.update {
                    it.copy(
                        availableAccounts = accounts.accounts,
                        selectedAccount = it.selectedAccount
                            ?: accounts.accounts.firstOrNull(),
                    )
                }
            }
        }
    }

    private fun loadCategories(type: TransactionType) {

        viewModelScope.launch {
            categoriesRepository.observeCategories()
                .map { categories ->
                    categories.filter {
                        it.isIncome == (type == TransactionType.INCOME)
                    }
                }
                .collect { categories ->
                    _uiState.update {
                        it.copy(
                            availableCategories = categories,
                            selectedCategory = categories.firstOrNull()
                        )
                    }
                }
        }

    }

    private fun saveTransaction() {
        val state = _uiState.value

        if (!state.canConfirm) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            // transactionsRepository.createTransaction(...)

            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
