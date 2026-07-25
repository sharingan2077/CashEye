package com.yandex.school.casheye.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.AnalyticsLoadResult
import com.yandex.school.casheye.domain.finance.AnalyticsQuery
import com.yandex.school.casheye.domain.finance.AnalyticsSummary
import com.yandex.school.casheye.domain.finance.AnalyticsTransactionKind
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.FinanceRefreshResult
import com.yandex.school.casheye.domain.finance.GetAnalyticsUseCase
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
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Suppress("TooManyFunctions")
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
    private var observeJob: Job? = null
    private var refreshJob: Job? = null
    private var latestSummary: AnalyticsSummary? = null
    private var initialRefreshCompleted = false
    private var localObservationReady = CompletableDeferred<Unit>()

    @Suppress("CyclomaticComplexMethod")
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
            AnalyticsIntent.Retry -> refreshAnalytics(currentQuery())
            AnalyticsIntent.Refresh -> refreshAnalytics(currentQuery())
        }
    }

    private fun initialize(entryPoint: AnalyticsEntryPoint) {
        if (initialized) return
        initialized = true
        val type = if (entryPoint == AnalyticsEntryPoint.Income) AnalyticsType.Income else AnalyticsType.Expenses
        screenData = initialScreenData(type)
        startAnalytics(currentQuery())
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

    @Suppress("ReturnCount")
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
        startAnalytics(currentQuery())
    }

    private fun updateSheet(sheet: AnalyticsSheet?) {
        screenData = screenData.copy(activeSheet = sheet)
        _state.value = _state.value.withData(screenData)
    }

    private fun currentQuery(): AnalyticsQuery {
        val filters = screenData.filters
        return AnalyticsQuery(
            startDate = filters.period.startDate,
            endDate = filters.period.endDate,
            currencyCode = CURRENCY_RUB,
            transactionKind = filters.type.toDomain(),
            accountId = filters.accountId,
            categoryIds = filters.categoryIds,
        )
    }

    private fun startAnalytics(query: AnalyticsQuery) {
        observeJob?.cancel()
        refreshJob?.cancel()
        latestSummary = null
        initialRefreshCompleted = false
        localObservationReady = CompletableDeferred()
        _state.value = AnalyticsUiState.Loading(screenData)
        val observationReady = localObservationReady
        observeJob =
            viewModelScope.launch {
                getAnalytics(query).collectLatest { result ->
                    observationReady.complete(Unit)
                    when (result) {
                        is AnalyticsLoadResult.Success -> {
                            latestSummary = result.summary
                            renderSummary()
                        }

                        is AnalyticsLoadResult.Failure -> {
                            if (!_state.value.isRefreshable()) {
                                _state.value = AnalyticsUiState.Error(screenData, result.reason)
                            }
                        }
                    }
                }
            }
        refreshAnalytics(query)
    }

    private fun refreshAnalytics(query: AnalyticsQuery) {
        refreshJob?.cancel()
        val observationReady = localObservationReady
        if (_state.value.isRefreshable()) {
            _state.value = _state.value.withRefreshing(true)
        }
        refreshJob =
            viewModelScope.launch {
                when (val result = getAnalytics.refresh(query)) {
                    FinanceRefreshResult.Success -> {
                        initialRefreshCompleted = true
                        renderSummary(isRefreshing = false)
                    }

                    is FinanceRefreshResult.Failure -> handleRefreshFailure(result.reason, observationReady)
                }
            }
    }

    private fun renderSummary(isRefreshing: Boolean = refreshJob?.isActive == true) {
        val summary = latestSummary ?: return
        val transactions = summary.transactions.sortedByDescending { it.transactionDate }
        if (transactions.isEmpty() && !initialRefreshCompleted) return
        screenData =
            screenData.copy(
                accounts = summary.accounts,
                categories = summary.availableCategories,
            )
        _state.value =
            if (transactions.isEmpty()) {
                AnalyticsUiState.Empty(screenData, summary.currencyCode, isRefreshing)
            } else {
                AnalyticsUiState.Content(
                    data = screenData,
                    total = summary.total,
                    currencyCode = summary.currencyCode,
                    transactions = transactions,
                    categorySummaries = transactions.toCategorySummaries(),
                    isRefreshing = isRefreshing,
                )
            }
    }

    private suspend fun handleRefreshFailure(
        reason: FinanceFailureReason,
        observationReady: CompletableDeferred<Unit>,
    ) {
        observationReady.await()
        initialRefreshCompleted = true
        val hasVisibleCache = _state.value.isRefreshable() || latestSummary?.transactions?.isNotEmpty() == true
        if (hasVisibleCache) {
            renderSummary(isRefreshing = false)
            _effects.emit(AnalyticsEffect.ShowError(reason))
        } else {
            _state.value = AnalyticsUiState.Error(screenData, reason)
        }
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

private fun AnalyticsUiState.isRefreshable(): Boolean =
    this is AnalyticsUiState.Content || this is AnalyticsUiState.Empty

private fun AnalyticsUiState.withRefreshing(isRefreshing: Boolean): AnalyticsUiState =
    when (this) {
        is AnalyticsUiState.Content -> copy(isRefreshing = isRefreshing)

        is AnalyticsUiState.Empty -> copy(isRefreshing = isRefreshing)

        is AnalyticsUiState.Loading,
        is AnalyticsUiState.Error,
        -> this
    }

private const val CURRENCY_RUB = "RUB"
