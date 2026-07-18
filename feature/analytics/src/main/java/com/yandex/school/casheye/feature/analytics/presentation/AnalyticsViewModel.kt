package com.yandex.school.casheye.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.AnalyticsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsQuery
import com.yandex.school.casheye.domain.finance.AnalyticsTransactionKind
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.GetAnalyticsUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Inject
class AnalyticsViewModel(
    private val getAnalytics: GetAnalyticsUseCase,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {
    private val today: LocalDate
        get() = LocalDate.now(clock)

    private var screenData = initialScreenData(AnalyticsType.Expenses)
    private val _state = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading(screenData))
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AnalyticsEffect>()
    val effects: SharedFlow<AnalyticsEffect> = _effects.asSharedFlow()

    private var initialized = false
    private var loadJob: Job? = null

    fun onIntent(intent: AnalyticsIntent) {
        when (intent) {
            is AnalyticsIntent.Initialize -> initialize(intent.entryPoint)
            is AnalyticsIntent.OpenFilter -> openFilter(intent.kind)
            AnalyticsIntent.DismissSheet -> updateSheet(null)
            is AnalyticsIntent.SelectDraftType -> selectDraftType(intent.type)
            AnalyticsIntent.ApplyDraftType -> applyDraftType()
            is AnalyticsIntent.SelectPeriodPreset -> applyPeriodPreset(intent.preset)
            AnalyticsIntent.OpenCustomPeriod -> openCustomPeriod()
            is AnalyticsIntent.UpdateCustomPeriod -> updateCustomPeriod(intent.startDate, intent.endDate)
            AnalyticsIntent.ApplyCustomPeriod -> applyCustomPeriod()
            is AnalyticsIntent.ToggleDraftCategory -> toggleDraftCategory(intent.categoryId)
            AnalyticsIntent.ApplyDraftCategories -> applyDraftCategories()
            is AnalyticsIntent.SelectAccount -> applyAccount(intent.accountId)
            AnalyticsIntent.OpenDetails -> updateSheet(AnalyticsSheet.Details)
            AnalyticsIntent.Retry -> loadAnalytics()
        }
    }

    private fun initialize(entryPoint: AnalyticsEntryPoint) {
        if (initialized) return
        initialized = true
        val type = if (entryPoint == AnalyticsEntryPoint.Income) AnalyticsType.Income else AnalyticsType.Expenses
        screenData = initialScreenData(type)
        loadAnalytics()
    }

    private fun initialScreenData(type: AnalyticsType): AnalyticsScreenData {
        val currentDate = today
        return AnalyticsScreenData(
            filters =
                AnalyticsFilters(
                    type = type,
                    period =
                        AnalyticsPeriod(
                            startDate = currentDate.withDayOfMonth(1),
                            endDate = currentDate,
                            preset = AnalyticsPeriodPreset.Month,
                        ),
                ),
            currentDate = currentDate,
        )
    }

    private fun openFilter(kind: AnalyticsFilterKind) {
        val sheet =
            when (kind) {
                AnalyticsFilterKind.Type -> AnalyticsSheet.Type(screenData.filters.type)
                AnalyticsFilterKind.Period -> AnalyticsSheet.Period
                AnalyticsFilterKind.Categories -> AnalyticsSheet.Categories(screenData.filters.categoryIds)
                AnalyticsFilterKind.Account -> AnalyticsSheet.Account
            }
        updateSheet(sheet)
    }

    private fun selectDraftType(type: AnalyticsType) {
        val sheet = screenData.activeSheet as? AnalyticsSheet.Type ?: return
        updateSheet(sheet.copy(selected = type))
    }

    private fun applyDraftType() {
        val sheet = screenData.activeSheet as? AnalyticsSheet.Type ?: return
        val filters =
            screenData.filters.copy(
                type = sheet.selected,
                categoryIds =
                    if (sheet.selected == screenData.filters.type) {
                        screenData.filters.categoryIds
                    } else {
                        emptySet()
                    },
            )
        applyFilters(filters)
    }

    private fun applyPeriodPreset(preset: AnalyticsPeriodPreset) {
        if (preset == AnalyticsPeriodPreset.Custom) {
            openCustomPeriod()
            return
        }
        val currentDate = today
        val startDate =
            when (preset) {
                AnalyticsPeriodPreset.Week -> {
                    currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                }

                AnalyticsPeriodPreset.Month -> {
                    currentDate.withDayOfMonth(1)
                }

                AnalyticsPeriodPreset.Quarter -> {
                    currentDate.withMonth(((currentDate.monthValue - 1) / 3) * 3 + 1).withDayOfMonth(1)
                }

                AnalyticsPeriodPreset.Year -> {
                    currentDate.withDayOfYear(1)
                }

                AnalyticsPeriodPreset.Custom -> {
                    error("Handled above")
                }
            }
        applyFilters(
            screenData.filters.copy(
                period = AnalyticsPeriod(startDate, currentDate, preset),
            ),
        )
    }

    private fun openCustomPeriod() {
        val period = screenData.filters.period
        updateSheet(AnalyticsSheet.CustomPeriod(period.startDate, period.endDate))
    }

    private fun updateCustomPeriod(
        startDate: LocalDate?,
        endDate: LocalDate?,
    ) {
        if (screenData.activeSheet !is AnalyticsSheet.CustomPeriod) return
        updateSheet(AnalyticsSheet.CustomPeriod(startDate, endDate))
    }

    private fun applyCustomPeriod() {
        val sheet = screenData.activeSheet as? AnalyticsSheet.CustomPeriod ?: return
        val startDate = sheet.startDate ?: return
        val endDate = sheet.endDate ?: return
        if (startDate > endDate || endDate > today) return
        applyFilters(
            screenData.filters.copy(
                period = AnalyticsPeriod(startDate, endDate, AnalyticsPeriodPreset.Custom),
            ),
        )
    }

    private fun toggleDraftCategory(categoryId: Int) {
        val sheet = screenData.activeSheet as? AnalyticsSheet.Categories ?: return
        val selected = sheet.selectedIds.toMutableSet()
        if (!selected.add(categoryId)) selected.remove(categoryId)
        updateSheet(sheet.copy(selectedIds = selected))
    }

    private fun applyDraftCategories() {
        val sheet = screenData.activeSheet as? AnalyticsSheet.Categories ?: return
        applyFilters(screenData.filters.copy(categoryIds = sheet.selectedIds))
    }

    private fun applyAccount(accountId: Int?) {
        applyFilters(screenData.filters.copy(accountId = accountId))
    }

    private fun applyFilters(filters: AnalyticsFilters) {
        screenData = screenData.copy(filters = filters, currentDate = today, activeSheet = null)
        loadAnalytics()
    }

    private fun updateSheet(sheet: AnalyticsSheet?) {
        screenData = screenData.copy(activeSheet = sheet)
        _state.value = _state.value.withData(screenData)
    }

    private fun loadAnalytics() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _state.value = AnalyticsUiState.Loading(screenData)
                val filters = screenData.filters
                when (
                    val result =
                        getAnalytics(
                            AnalyticsQuery(
                                startDate = filters.period.startDate,
                                endDate = filters.period.endDate,
                                currencyCode = CURRENCY_RUB,
                                transactionKind = filters.type.toDomain(),
                                accountId = filters.accountId,
                                categoryIds = filters.categoryIds,
                            ),
                        )
                ) {
                    is AnalyticsLoadResult.Success -> handleSuccess(result)
                    is AnalyticsLoadResult.Failure -> handleFailure(result.reason)
                }
            }
    }

    private fun handleSuccess(result: AnalyticsLoadResult.Success) {
        val summary = result.summary
        val transactions = summary.transactions.sortedByDescending { it.transactionDate }
        screenData =
            screenData.copy(
                accounts = summary.accounts,
                categories = summary.availableCategories,
                activeSheet = null,
            )
        _state.value =
            if (transactions.isEmpty()) {
                AnalyticsUiState.Empty(screenData, summary.currencyCode)
            } else {
                AnalyticsUiState.Content(
                    data = screenData,
                    total = summary.total,
                    currencyCode = summary.currencyCode,
                    transactions = transactions,
                    categorySummaries = transactions.toCategorySummaries(),
                )
            }
    }

    private suspend fun handleFailure(reason: FinanceFailureReason) {
        val message = reason.toUserMessage()
        _state.value = AnalyticsUiState.Error(screenData, message)
        _effects.emit(AnalyticsEffect.ShowError(message))
    }
}

private fun AnalyticsType.toDomain(): AnalyticsTransactionKind =
    when (this) {
        AnalyticsType.Expenses -> AnalyticsTransactionKind.Expense
        AnalyticsType.Income -> AnalyticsTransactionKind.Income
        AnalyticsType.All -> AnalyticsTransactionKind.All
    }

private fun List<com.yandex.school.casheye.core.model.Transaction>.toCategorySummaries():
    List<AnalyticsCategorySummary> =
    groupBy { it.category.id }
        .values
        .map { transactions ->
            AnalyticsCategorySummary(
                category = transactions.first().category,
                amount = transactions.fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount },
            )
        }.sortedByDescending { it.amount }

private fun AnalyticsUiState.withData(data: AnalyticsScreenData): AnalyticsUiState =
    when (this) {
        is AnalyticsUiState.Loading -> copy(data = data)
        is AnalyticsUiState.Empty -> copy(data = data)
        is AnalyticsUiState.Content -> copy(data = data)
        is AnalyticsUiState.Error -> copy(data = data)
    }

private fun FinanceFailureReason.toUserMessage(): String =
    when (this) {
        FinanceFailureReason.Network -> "Проверьте подключение к интернету"
        FinanceFailureReason.Authorization -> "Не удалось авторизоваться"
        FinanceFailureReason.Server -> "Сервер временно недоступен"
        FinanceFailureReason.Unknown -> "Не удалось загрузить аналитику"
    }

private const val CURRENCY_RUB = "RUB"
