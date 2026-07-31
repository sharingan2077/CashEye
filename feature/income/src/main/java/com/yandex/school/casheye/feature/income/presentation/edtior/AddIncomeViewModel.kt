package com.yandex.school.casheye.feature.income.presentation.edtior

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.FinanceEditorInputLimits
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.GetEditorAccountsUseCase
import com.yandex.school.casheye.domain.finance.GetEditorCategoriesUseCase
import com.yandex.school.casheye.domain.finance.GetTransactionUseCase
import com.yandex.school.casheye.domain.finance.SaveTransactionUseCase
import com.yandex.school.casheye.domain.finance.editor.EditorResult
import com.yandex.school.casheye.domain.finance.editor.SaveTransactionCommand
import com.yandex.school.casheye.feature.income.R
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@Inject
class AddIncomeViewModel(
    private val getAccounts: GetEditorAccountsUseCase,
    private val getCategories: GetEditorCategoriesUseCase,
    private val getTransaction: GetTransactionUseCase,
    private val saveTransaction: SaveTransactionUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val _state = MutableStateFlow(AddIncomeUiState())
    val state = _state.asStateFlow()
    private val _effects = MutableSharedFlow<AddIncomeEffect>()
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: AddIncomeIntent) {
        when (intent) {
            is AddIncomeIntent.Open -> {
                load(intent)
            }

            is AddIncomeIntent.AmountChanged -> {
                updateAmount(intent.value)
            }

            is AddIncomeIntent.AccountSelected -> {
                _state.value =
                    _state.value.copy(selectedAccountId = intent.id, error = null)
            }

            is AddIncomeIntent.CategorySelected -> {
                _state.value =
                    _state.value.copy(selectedCategoryId = intent.id, error = null)
            }

            is AddIncomeIntent.DateChanged -> {
                _state.value = _state.value.copy(date = intent.value.coerceAtMost(LocalDate.now(clock)))
            }

            is AddIncomeIntent.TimeChanged -> {
                _state.value = _state.value.copy(time = intent.value)
            }

            is AddIncomeIntent.CommentChanged -> {
                _state.value =
                    _state.value.copy(
                        comment = intent.value.take(FinanceEditorInputLimits.TRANSACTION_COMMENT_MAX_LENGTH),
                    )
            }

            AddIncomeIntent.Save -> {
                save()
            }
        }
    }

    private fun load(intent: AddIncomeIntent.Open) {
        viewModelScope.launch {
            val today = LocalDate.now(clock)
            _state.value =
                AddIncomeUiState(
                    editingId = intent.transactionId,
                    date = intent.defaultDate.coerceAtMost(today),
                    time = LocalTime.now(clock).withSecond(0).withNano(0),
                )
            val accountResult = getAccounts()
            val categoryResult = getCategories(true)
            val accounts = (accountResult as? EditorResult.Success)?.value
            val categories = (categoryResult as? EditorResult.Success)?.value
            if (accounts == null || categories == null) {
                val reason =
                    (accountResult as? EditorResult.Failure)?.reason
                        ?: (categoryResult as EditorResult.Failure).reason
                _state.value = _state.value.copy(isLoading = false, error = reason.editorMessage())
                return@launch
            }
            if (accounts.isEmpty()) {
                _state.value = _state.value.copy(isLoading = false, error = R.string.error_create_account)
                return@launch
            }
            val transaction =
                intent.transactionId?.let { id ->
                    when (val result = getTransaction(id)) {
                        is EditorResult.Success -> {
                            result.value
                        }

                        is EditorResult.Failure -> {
                            _state.value = _state.value.copy(isLoading = false, error = result.reason.editorMessage())
                            return@launch
                        }
                    }
                }
            val local = transaction?.transactionDate?.atZone(ZoneId.systemDefault())
            _state.value =
                _state.value.copy(
                    isLoading = false,
                    accounts = accounts,
                    categories = categories,
                    selectedAccountId = transaction?.account?.id ?: accounts.first().id,
                    selectedCategoryId = transaction?.category?.id,
                    amount = transaction?.amount?.toPlainString().orEmpty(),
                    date = (local?.toLocalDate() ?: intent.defaultDate).coerceAtMost(today),
                    time = local?.toLocalTime()?.withSecond(0)?.withNano(0) ?: _state.value.time,
                    comment =
                        transaction
                            ?.comment
                            .orEmpty()
                            .take(FinanceEditorInputLimits.TRANSACTION_COMMENT_MAX_LENGTH),
                )
        }
    }

    private fun updateAmount(value: String) {
        val normalized = value.replace(',', '.')
        if (
            normalized.count { it == '.' } <= 1 &&
            normalized.all { it.isDigit() || it == '.' } &&
            normalized.substringAfter('.', "").length <= 2
        ) {
            _state.value = _state.value.copy(amount = normalized, error = null)
        }
    }

    private fun save() {
        val state = _state.value
        if (state.isSaving) return
        val amount = state.amount.toBigDecimalOrNull()
        val validation =
            when {
                amount == null || amount <= BigDecimal.ZERO -> R.string.error_positive_amount
                state.selectedAccountId == null -> R.string.error_select_account
                state.selectedCategoryId == null -> R.string.error_select_category
                else -> null
            }
        if (validation != null) {
            _state.value = state.copy(error = validation)
            return
        }
        viewModelScope.launch {
            _state.value = state.copy(isSaving = true, error = null)
            val result =
                saveTransaction(
                    SaveTransactionCommand(
                        id = state.editingId,
                        accountId = requireNotNull(state.selectedAccountId),
                        categoryId = requireNotNull(state.selectedCategoryId),
                        amount = requireNotNull(amount),
                        transactionDate =
                            state.date
                                .atTime(state.time)
                                .atZone(ZoneId.systemDefault())
                                .toInstant(),
                        comment = state.comment.trim().ifBlank { null },
                    ),
                )
            when (result) {
                is EditorResult.Success -> {
                    _effects.emit(AddIncomeEffect.Saved)
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
        FinanceFailureReason.Unknown -> R.string.error_save_income
    }
