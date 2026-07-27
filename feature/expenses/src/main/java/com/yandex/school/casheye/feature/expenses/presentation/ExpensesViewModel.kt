package com.yandex.school.casheye.feature.expenses.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.MoneyAmount
import com.yandex.school.casheye.domain.finance.DeleteTransactionUseCase
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceLoadResult
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.FinanceSummary
import com.yandex.school.casheye.domain.finance.GetDailySummaryUseCase
import com.yandex.school.casheye.domain.finance.TransactionKind
import com.yandex.school.casheye.domain.finance.editor.EditorResult
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
import java.time.Clock
import java.time.LocalDate

@Inject
class ExpensesViewModel(
    private val getDailySummary: GetDailySummaryUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val _state = MutableStateFlow<ExpensesUiState>(ExpensesUiState.Loading)
    val state: StateFlow<ExpensesUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<ExpensesEffect>()
    val effects: SharedFlow<ExpensesEffect> = _effects.asSharedFlow()

    private var observeJob: Job? = null
    private var refreshJob: Job? = null
    private var latestSummary: FinanceSummary? = null
    private var initialRefreshCompleted = false
    private var localObservationReady = CompletableDeferred<Unit>()
    private var selectedDate = LocalDate.now(clock)

    init {
        observeExpenses(selectedDate)
        refreshExpenses(selectedDate)
    }

    fun onIntent(intent: ExpensesIntent) {
        when (intent) {
            ExpensesIntent.Retry -> refreshExpenses(selectedDate)
            ExpensesIntent.Refresh -> refreshExpenses(selectedDate)
            is ExpensesIntent.SelectDate -> selectDate(intent.date)
            is ExpensesIntent.DeleteTransaction -> deleteTransaction(intent.id)
        }
    }

    private fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            when (val result = deleteTransactionUseCase(id)) {
                is EditorResult.Success -> {
                    removeTransaction(id)
                    _effects.emit(ExpensesEffect.TransactionDeleted)
                }

                is EditorResult.Failure -> {
                    _effects.emit(ExpensesEffect.ShowDeleteError(result.reason))
                }
            }
        }
    }

    private fun removeTransaction(id: Int) {
        val content = _state.value as? ExpensesUiState.Content ?: return
        val removed = content.transactions.firstOrNull { it.id == id } ?: return
        val remaining = content.transactions.filterNot { it.id == id }
        _state.value =
            if (remaining.isEmpty()) {
                ExpensesUiState.Empty()
            } else {
                content.copy(
                    nativeTotals =
                        content.nativeTotals.subtract(
                            amount = MoneyAmount(removed.amount, removed.currency),
                        ),
                    transactions = remaining,
                    isRefreshing = false,
                )
            }
    }

    private fun selectDate(date: LocalDate) {
        val selectableDate = date.coerceAtMost(LocalDate.now(clock))
        if (selectableDate == selectedDate) return

        selectedDate = selectableDate
        latestSummary = null
        initialRefreshCompleted = false
        localObservationReady = CompletableDeferred()
        _state.value = ExpensesUiState.Loading
        observeExpenses(selectableDate)
        refreshExpenses(selectableDate)
    }

    private fun observeExpenses(date: LocalDate) {
        observeJob?.cancel()
        val observationReady = localObservationReady
        observeJob =
            viewModelScope.launch {
                getDailySummary(
                    date = date,
                    transactionKind = TransactionKind.Expense,
                ).collectLatest { result ->
                    observationReady.complete(Unit)
                    when (result) {
                        is FinanceLoadResult.Success -> {
                            latestSummary = result.summary
                            renderSummary()
                        }

                        is FinanceLoadResult.Failure -> {
                            if (!_state.value.isRefreshable()) {
                                _state.value = ExpensesUiState.Error(result.reason)
                            }
                        }
                    }
                }
            }
    }

    private fun refreshExpenses(date: LocalDate) {
        refreshJob?.cancel()
        val observationReady = localObservationReady
        if (_state.value.isRefreshable()) {
            _state.value = _state.value.withRefreshing(true)
        }
        refreshJob =
            viewModelScope.launch {
                when (val result = getDailySummary.refresh(date)) {
                    FinanceRefreshResult.Success -> {
                        initialRefreshCompleted = true
                        renderSummary(isRefreshing = false)
                    }

                    is FinanceRefreshResult.Failure -> {
                        observationReady.await()
                        initialRefreshCompleted = true
                        val hasVisibleCache =
                            _state.value.isRefreshable() || latestSummary?.transactions?.isNotEmpty() == true
                        if (
                            result.reason == FinanceFailureReason.Network &&
                            result.hasUsableCache &&
                            latestSummary != null
                        ) {
                            renderSummary(isRefreshing = false)
                        } else if (hasVisibleCache) {
                            renderSummary(isRefreshing = false)
                            _effects.emit(ExpensesEffect.ShowError(result.reason))
                        } else {
                            _state.value = ExpensesUiState.Error(result.reason)
                        }
                    }
                }
            }
    }

    private fun renderSummary(isRefreshing: Boolean = refreshJob?.isActive == true) {
        val summary = latestSummary ?: return
        if (summary.transactions.isEmpty() && !initialRefreshCompleted) return
        _state.value =
            if (summary.transactions.isEmpty()) {
                ExpensesUiState.Empty(isRefreshing)
            } else {
                ExpensesUiState.Content(
                    nativeTotals = summary.nativeTotals,
                    transactions = summary.transactions,
                    isRefreshing = isRefreshing,
                )
            }
    }
}

private fun List<MoneyAmount>.subtract(amount: MoneyAmount): List<MoneyAmount> =
    mapNotNull { total ->
        if (total.currency != amount.currency) {
            total
        } else {
            total
                .copy(amount = total.amount - amount.amount)
                .takeUnless { it.amount.signum() == 0 }
        }
    }

private fun ExpensesUiState.isRefreshable(): Boolean = this is ExpensesUiState.Content || this is ExpensesUiState.Empty

private fun ExpensesUiState.withRefreshing(isRefreshing: Boolean): ExpensesUiState =
    when (this) {
        is ExpensesUiState.Content -> copy(isRefreshing = isRefreshing)

        is ExpensesUiState.Empty -> copy(isRefreshing = isRefreshing)

        ExpensesUiState.Loading,
        is ExpensesUiState.Error,
        -> this
    }
