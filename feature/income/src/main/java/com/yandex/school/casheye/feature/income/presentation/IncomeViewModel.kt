package com.yandex.school.casheye.feature.income.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.core.model.DatePeriod
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
class IncomeViewModel(
    private val getIncome: GetDailySummaryUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val _state = MutableStateFlow<IncomeUiState>(IncomeUiState.Loading)
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<IncomeEffect>()

    val effects: SharedFlow<IncomeEffect> = _effects.asSharedFlow()

    private var observeJob: Job? = null
    private var refreshJob: Job? = null
    private var latestSummary: FinanceSummary? = null
    private var initialRefreshCompleted = false
    private var localObservationReady = CompletableDeferred<Unit>()

    private var selectedPeriod = DatePeriod(LocalDate.now(clock), LocalDate.now(clock))

    init {
        observeIncome(selectedPeriod)
        refreshIncome(selectedPeriod)
    }

    fun onIntent(intent: IncomeIntent) {
        when (intent) {
            IncomeIntent.Retry -> refreshIncome(selectedPeriod)
            IncomeIntent.Refresh -> refreshIncome(selectedPeriod)
            IncomeIntent.NetworkRecovered -> refreshIncome(selectedPeriod, showLoadingForEmptyCache = true)
            is IncomeIntent.SelectDate -> selectPeriod(DatePeriod(intent.date, intent.date))
            is IncomeIntent.SelectPeriod -> selectPeriod(intent.period)
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

                is EditorResult.Failure -> {
                    _effects.emit(IncomeEffect.ShowDeleteError(result.reason))
                }
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
                    nativeTotals =
                        content.nativeTotals.subtract(
                            amount = MoneyAmount(removed.amount, removed.currency),
                        ),
                    transactions = remaining,
                    currentValuation = null,
                    isRefreshing = false,
                )
            }
    }

    private fun selectPeriod(period: DatePeriod) {
        val today = LocalDate.now(clock)
        val selectablePeriod =
            DatePeriod(
                period.startDate.coerceAtMost(today),
                period.endDate.coerceAtMost(today),
            )
        if (selectablePeriod.startDate > selectablePeriod.endDate || selectablePeriod == selectedPeriod) return

        selectedPeriod = selectablePeriod
        latestSummary = null
        initialRefreshCompleted = false
        localObservationReady = CompletableDeferred()
        _state.value = IncomeUiState.Loading
        observeIncome(selectedPeriod)
        refreshIncome(selectedPeriod)
    }

    private fun observeIncome(period: DatePeriod) {
        observeJob?.cancel()
        val observationReady = localObservationReady
        observeJob =
            viewModelScope.launch {
                getIncome(
                    startDate = period.startDate,
                    endDate = period.endDate,
                    transactionKind = TransactionKind.Income,
                ).collectLatest { result ->
                    observationReady.complete(Unit)
                    when (result) {
                        is FinanceLoadResult.Success -> {
                            latestSummary = result.summary
                            renderSummary()
                        }

                        is FinanceLoadResult.Failure -> {
                            if (!_state.value.isRefreshable()) {
                                _state.value = IncomeUiState.Error(result.reason)
                            }
                        }
                    }
                }
            }
    }

    private fun refreshIncome(
        period: DatePeriod,
        showLoadingForEmptyCache: Boolean = false,
    ) {
        refreshJob?.cancel()
        val observationReady = localObservationReady
        if (_state.value.isRefreshable()) {
            _state.value = _state.value.withRefreshing(true)
        } else if (showLoadingForEmptyCache) {
            _state.value = IncomeUiState.Loading
        }
        refreshJob =
            viewModelScope.launch {
                when (val result = getIncome.refresh(period.startDate, period.endDate)) {
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
                            _effects.emit(IncomeEffect.ShowError(result.reason))
                        } else {
                            _state.value = IncomeUiState.Error(result.reason)
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
                IncomeUiState.Empty(isRefreshing)
            } else {
                IncomeUiState.Content(
                    nativeTotals = summary.nativeTotals,
                    transactions = summary.transactions,
                    currentValuation = summary.currentValuation,
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

private fun IncomeUiState.isRefreshable(): Boolean = this is IncomeUiState.Content || this is IncomeUiState.Empty

private fun IncomeUiState.withRefreshing(isRefreshing: Boolean): IncomeUiState =
    when (this) {
        is IncomeUiState.Content -> copy(isRefreshing = isRefreshing)

        is IncomeUiState.Empty -> copy(isRefreshing = isRefreshing)

        IncomeUiState.Loading,
        is IncomeUiState.Error,
        -> this
    }
