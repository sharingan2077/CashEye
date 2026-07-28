package com.yandex.school.casheye.feature.settings.presentation

import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.settings.AppLanguage
import com.yandex.school.casheye.domain.settings.AppSettings
import com.yandex.school.casheye.domain.settings.ThemeMode

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val reportingCurrency: CurrencyCode = CurrencyCode.RUB,
    val destination: SettingsDestination = SettingsDestination.Root,
    val articles: List<Category> = emptyList(),
    val articlesQuery: String = "",
    val isArticlesLoading: Boolean = false,
    val articlesError: FinanceFailureReason? = null,
) {
    val visibleArticles: List<Category>
        get() = articles.filter { it.name.contains(articlesQuery, ignoreCase = true) }
}

sealed interface SettingsIntent {
    data class OpenDestination(
        val destination: SettingsDestination,
    ) : SettingsIntent

    data object BackToRoot : SettingsIntent

    data class SelectReportingCurrency(
        val currency: CurrencyCode,
    ) : SettingsIntent

    data class SelectThemeMode(
        val mode: ThemeMode,
    ) : SettingsIntent

    data class SelectLanguage(
        val language: AppLanguage,
    ) : SettingsIntent

    data class ArticlesQueryChanged(
        val value: String,
    ) : SettingsIntent

    data object LoadArticles : SettingsIntent

    data object Reset : SettingsIntent
}

sealed interface SettingsDestination {
    data object Root : SettingsDestination

    data object Currency : SettingsDestination

    data object Articles : SettingsDestination

    data object Appearance : SettingsDestination

    data object Language : SettingsDestination

    data object Pin : SettingsDestination

    data object Biometrics : SettingsDestination
}
