package com.yandex.school.casheye.feature.analytics.presentation

import com.yandex.school.casheye.core.model.Account
import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.Transaction
import java.math.BigDecimal
import java.time.LocalDate

enum class AnalyticsEntryPoint {
    Expenses,
    Income,
    Accounts,
}

enum class AnalyticsType(
    val title: String,
) {
    Expenses("Расходы"),
    Income("Доходы"),
    All("Всё"),
}

enum class AnalyticsPeriodPreset(
    val title: String,
) {
    Custom("Произвольный"),
    Week("За неделю"),
    Month("За месяц"),
    Quarter("За квартал"),
    Year("За год"),
}

data class AnalyticsPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val preset: AnalyticsPeriodPreset,
)

data class AnalyticsFilters(
    val type: AnalyticsType,
    val period: AnalyticsPeriod,
    val accountId: Int? = null,
    val categoryIds: Set<Int> = emptySet(),
)

enum class AnalyticsFilterKind {
    Type,
    Period,
    Categories,
    Account,
}

sealed interface AnalyticsSheet {
    data class Type(
        val selected: AnalyticsType,
    ) : AnalyticsSheet

    data object Period : AnalyticsSheet

    data class CustomPeriod(
        val startDate: LocalDate?,
        val endDate: LocalDate?,
    ) : AnalyticsSheet

    data class Categories(
        val selectedIds: Set<Int>,
    ) : AnalyticsSheet

    data object Account : AnalyticsSheet

    data object Details : AnalyticsSheet
}

data class AnalyticsScreenData(
    val filters: AnalyticsFilters,
    val currentDate: LocalDate,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val activeSheet: AnalyticsSheet? = null,
)

data class AnalyticsCategorySummary(
    val category: Category,
    val amount: BigDecimal,
)

sealed interface AnalyticsUiState {
    val data: AnalyticsScreenData

    data class Loading(
        override val data: AnalyticsScreenData,
    ) : AnalyticsUiState

    data class Empty(
        override val data: AnalyticsScreenData,
        val currencyCode: String,
    ) : AnalyticsUiState

    data class Content(
        override val data: AnalyticsScreenData,
        val total: BigDecimal,
        val currencyCode: String,
        val transactions: List<Transaction>,
        val categorySummaries: List<AnalyticsCategorySummary>,
    ) : AnalyticsUiState

    data class Error(
        override val data: AnalyticsScreenData,
        val message: String,
    ) : AnalyticsUiState
}

sealed interface AnalyticsIntent {
    data class Initialize(
        val entryPoint: AnalyticsEntryPoint,
    ) : AnalyticsIntent

    data class OpenFilter(
        val kind: AnalyticsFilterKind,
    ) : AnalyticsIntent

    data object DismissSheet : AnalyticsIntent

    data class SelectDraftType(
        val type: AnalyticsType,
    ) : AnalyticsIntent

    data object ApplyDraftType : AnalyticsIntent

    data class SelectPeriodPreset(
        val preset: AnalyticsPeriodPreset,
    ) : AnalyticsIntent

    data object OpenCustomPeriod : AnalyticsIntent

    data class UpdateCustomPeriod(
        val startDate: LocalDate?,
        val endDate: LocalDate?,
    ) : AnalyticsIntent

    data object ApplyCustomPeriod : AnalyticsIntent

    data class ToggleDraftCategory(
        val categoryId: Int,
    ) : AnalyticsIntent

    data object ApplyDraftCategories : AnalyticsIntent

    data class SelectAccount(
        val accountId: Int?,
    ) : AnalyticsIntent

    data object OpenDetails : AnalyticsIntent

    data object Retry : AnalyticsIntent
}

sealed interface AnalyticsEffect {
    data class ShowError(
        val message: String,
    ) : AnalyticsEffect
}
