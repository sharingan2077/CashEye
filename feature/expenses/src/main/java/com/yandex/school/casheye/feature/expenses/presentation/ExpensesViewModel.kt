package com.yandex.school.casheye.feature.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.TransactionKind
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

@Inject
class ExpensesViewModel(
    private val getDailySummary: GetDailySummaryUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val _state = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ExpensesEffect>()
    val effects: SharedFlow<ExpensesEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null
    private var selectedDate = LocalDate.now(clock)

    init {
        loadExpenses(selectedDate)
    }

    fun onIntent(intent: ExpensesIntent) {
        when (intent) {
            ExpensesIntent.Retry -> loadExpenses(selectedDate)
            is ExpensesIntent.SelectDate -> selectDate(intent.date)
        }
    }

    private fun selectDate(date: LocalDate) {
        if (date == selectedDate) return

        selectedDate = date
        loadExpenses(date)
    }

    private fun loadExpenses(date: LocalDate) {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _state.value = ExpensesUiState.Loading
                when (
                    val result =
                        getDailySummary(
                            date = date,
                            currencyCode = CURRENCY_RUB,
                            transactionKind = TransactionKind.Expense,
                        )
                ) {
                    is FinanceLoadResult.Success -> {
                        val summary = result.summary
                        _state.value =
                            if (summary.transactions.isEmpty()) {
                                ExpensesUiState.Empty
                            } else {
                                ExpensesUiState.Content(
                                    total = summary.total,
                                    currencyCode = summary.currencyCode,
                                    transactions = summary.transactions,
                                )
                            }
                    }

                    is FinanceLoadResult.Failure -> {
                        val message = result.reason.toUserMessage()
                        _state.value = ExpensesUiState.Error(message)
                        _effects.emit(ExpensesEffect.ShowError(message))
                    }
                }
            }
    }
}

private fun FinanceFailureReason.toUserMessage(): String =
    when (this) {
        FinanceFailureReason.Network -> "Проверьте подключение к интернету"
        FinanceFailureReason.Authorization -> "Не удалось авторизоваться"
        FinanceFailureReason.Server -> "Сервер временно недоступен"
        FinanceFailureReason.Unknown -> "Не удалось загрузить расходы"
    }

private const val CURRENCY_RUB = "RUB"
