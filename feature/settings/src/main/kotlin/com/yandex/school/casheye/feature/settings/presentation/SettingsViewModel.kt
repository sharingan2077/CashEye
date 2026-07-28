package com.yandex.school.casheye.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.finance.GetEditorCategoriesUseCase
import com.yandex.school.casheye.domain.finance.currency.ObserveReportingCurrencyUseCase
import com.yandex.school.casheye.domain.finance.currency.SetReportingCurrencyUseCase
import com.yandex.school.casheye.domain.finance.editor.EditorResult
import com.yandex.school.casheye.domain.settings.ObserveSettingsUseCase
import com.yandex.school.casheye.domain.settings.SetLanguageUseCase
import com.yandex.school.casheye.domain.settings.SetThemeModeUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
    observeReportingCurrency: ObserveReportingCurrencyUseCase,
    private val setReportingCurrency: SetReportingCurrencyUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val setLanguage: SetLanguageUseCase,
    private val getCategories: GetEditorCategoriesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }
        viewModelScope.launch {
            observeReportingCurrency().collect { currency ->
                _state.value = _state.value.copy(reportingCurrency = currency)
            }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.OpenDestination -> {
                _state.value = _state.value.copy(destination = intent.destination)
                if (intent.destination == SettingsDestination.Articles) loadArticles()
            }

            is SettingsIntent.SelectReportingCurrency -> {
                _state.value = _state.value.copy(destination = SettingsDestination.Root)
                viewModelScope.launch { setReportingCurrency(intent.currency) }
            }

            is SettingsIntent.SelectThemeMode -> {
                viewModelScope.launch { setThemeMode(intent.mode) }
            }

            is SettingsIntent.SelectLanguage -> {
                viewModelScope.launch { setLanguage(intent.language) }
            }

            is SettingsIntent.ArticlesQueryChanged -> {
                _state.value = _state.value.copy(articlesQuery = intent.value)
            }

            SettingsIntent.LoadArticles -> loadArticles()

            SettingsIntent.BackToRoot,
            SettingsIntent.Reset,
            -> _state.value = _state.value.copy(destination = SettingsDestination.Root)
        }
    }

    private fun loadArticles() {
        if (_state.value.isArticlesLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isArticlesLoading = true, articlesError = null)
            val expenseResult = getCategories(isIncome = false)
            val incomeResult = getCategories(isIncome = true)
            val expenseCategories = (expenseResult as? EditorResult.Success)?.value
            val incomeCategories = (incomeResult as? EditorResult.Success)?.value
            val updatedArticles =
                _state.value.articles
                    .filter { category ->
                        category.isIncome && incomeCategories == null ||
                            !category.isIncome && expenseCategories == null
                    }.let { cachedCategories ->
                        cachedCategories + expenseCategories.orEmpty() + incomeCategories.orEmpty()
                    }
            if (expenseCategories != null && incomeCategories != null) {
                _state.value =
                    _state.value.copy(
                        articles = updatedArticles,
                        isArticlesLoading = false,
                    )
            } else {
                _state.value =
                    _state.value.copy(
                        articles = updatedArticles,
                        isArticlesLoading = false,
                        articlesError =
                            (expenseResult as? EditorResult.Failure)?.reason
                                ?: (incomeResult as? EditorResult.Failure)?.reason,
                    )
            }
        }
    }
}
