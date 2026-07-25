package com.yandex.school.casheye.feature.income.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.DeleteTransactionUseCase
import com.yandex.school.casheye.domain.finance.EditorResult
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
class IncomeViewModel(
    private val getIncome: GetDailySummaryUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val _state = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<IncomeEffect>()

    val effects: SharedFlow<IncomeEffect> = _effects.asSharedFlow()

    private var loadJob: Job? = null

    private var selectedDate = LocalDate.now(clock)

    init {
        loadData(selectedDate)
    }

    fun onIntent(intent: IncomeIntent) {
        when (intent) {
            IncomeIntent.Retry -> loadData(selectedDate, preserveContent = _state.value.isRefreshable())
            IncomeIntent.Refresh -> loadData(selectedDate, preserveContent = true)
            is IncomeIntent.SelectDate -> selectDate(intent.date)
            is IncomeIntent.DeleteTransaction -> deleteTransaction(intent.id)
        }
    }

    private fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase(id)) {
                is EditorResult.Success -> {
                    removeTransaction(id)
                    _effects.emit(IncomeEffect.TransactionDeleted)
                }

                is EditorResult.Failure -> _effects.emit(IncomeEffect.ShowDeleteError(result.reason))
            }
        }
    }

    private fun removeTransaction(id: Int) {
        val content = _state.value as? IncomeUiState.Content ?: return
        val removed = content.transactions.firstOrNull { it.id == id } ?: return
        val remaining = content.transactions.filterNot { it.id == id }
        _state.value =
            if (remaining.isEmpty()) {
                IncomeUiState.Empty()
            } else {
                content.copy(
                    total = content.total.subtract(removed.amount),
                    transactions = remaining,
                    isRefreshing = false,
                )
            }
    }

    private fun selectDate(date: LocalDate) {
        val selectableDate = date.coerceAtMost(LocalDate.now(clock))
        if (selectableDate == selectedDate) return

        selectedDate = selectableDate
        loadData(selectedDate, cancelPrevious = true)
    }

    private fun loadData(
        localDate: LocalDate,
        preserveContent: Boolean = false,
        cancelPrevious: Boolean = false,
    ) {
        if (cancelPrevious) {
            loadJob?.cancel()
        } else if (loadJob?.isActive == true) {
            return
        }
        val keepsVisibleContent = preserveContent && _state.value.isRefreshable()
        loadJob =
            viewModelScope.launch {
                _state.value =
                    if (keepsVisibleContent) {
                        _state.value.withRefreshing(true)
                    } else {
                        IncomeUiState.Loading
                    }

                when (
                    val result =
                        getIncome(
                            date = localDate,
                            currencyCode = CURRENCY_CODE,
                            transactionKind = TransactionKind.Income,
                        )
                ) {
                    is FinanceLoadResult.Success -> {
                        val summary = result.summary
                        _state.value =
                            if (summary.transactions.isEmpty()) {
                                IncomeUiState.Empty()
                            } else {
                                IncomeUiState.Content(
                                    total = summary.total,
                                    currencyCode = summary.currencyCode,
                                    transactions = summary.transactions,
                                )
                            }
                    }

                    is FinanceLoadResult.Failure -> {
                        _state.value =
                            if (keepsVisibleContent) {
                                _state.value.withRefreshing(false)
                            } else {
                                IncomeUiState.Error(result.reason)
                            }
                        if (keepsVisibleContent) {
                            _effects.emit(IncomeEffect.ShowError(result.reason))
                        }
                    }
                }
            }
    }
}

private fun IncomeUiState.isRefreshable(): Boolean = this is IncomeUiState.Content || this is IncomeUiState.Empty

private fun IncomeUiState.withRefreshing(isRefreshing: Boolean): IncomeUiState =
    when (this) {
        is IncomeUiState.Content -> copy(isRefreshing = isRefreshing)

        is IncomeUiState.Empty -> copy(isRefreshing = isRefreshing)

        IncomeUiState.Loading,
        is IncomeUiState.Error,
        -> this
    }

private const val CURRENCY_CODE = "RUB"
