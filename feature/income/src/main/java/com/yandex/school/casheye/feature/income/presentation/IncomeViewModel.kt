package com.yandex.school.casheye.feature.income.presentation

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
class IncomeViewModel(
    private val getIncome: GetDailySummaryUseCase,
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
            IncomeIntent.Retry -> loadData(selectedDate)
            is IncomeIntent.SelectDate -> selectDate(intent.date)
        }
    }

    private fun selectDate(date: LocalDate) {
        if (date == selectedDate) return

        selectedDate = date
        loadData(selectedDate)
    }

    private fun loadData(localDate: LocalDate) {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _state.value = IncomeUiState.Loading

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
                                IncomeUiState.Empty
                            } else {
                                IncomeUiState.Content(
                                    total = summary.total,
                                    currencyCode = summary.currencyCode,
                                    transactions = summary.transactions,
                                )
                            }
                    }

                    is FinanceLoadResult.Failure -> {
                        val message = result.reason.toUserMessage()
                        _state.value = IncomeUiState.Error(message)
                        _effects.emit(IncomeEffect.ShowError(message))
                    }
                }
            }
    }
}

fun FinanceFailureReason.toUserMessage(): String =
    when (this) {
        FinanceFailureReason.Network -> "Проверьте подключение к интернету"
        FinanceFailureReason.Authorization -> "Не удалось авторизоваться"
        FinanceFailureReason.Server -> "Сервер временно недоступен"
        FinanceFailureReason.Unknown -> "Не удалось загрузить доходы"
    }

private const val CURRENCY_CODE = "RUB"
