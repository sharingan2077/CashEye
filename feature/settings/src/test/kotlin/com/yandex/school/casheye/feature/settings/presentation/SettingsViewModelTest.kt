package com.yandex.school.casheye.feature.settings.presentation

import com.yandex.school.casheye.core.model.Category
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.domain.finance.FinanceEditorRepository
import com.yandex.school.casheye.domain.finance.FinanceFailureReason
import com.yandex.school.casheye.domain.finance.GetEditorCategoriesUseCase
import com.yandex.school.casheye.domain.finance.currency.ObserveReportingCurrencyUseCase
import com.yandex.school.casheye.domain.finance.currency.ReportingCurrencyRepository
import com.yandex.school.casheye.domain.finance.currency.SetReportingCurrencyUseCase
import com.yandex.school.casheye.domain.finance.editor.EditorResult
import com.yandex.school.casheye.domain.settings.AppLanguage
import com.yandex.school.casheye.domain.settings.AppSettings
import com.yandex.school.casheye.domain.settings.ObserveSettingsUseCase
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.domain.settings.SetBiometricsEnabledUseCase
import com.yandex.school.casheye.domain.settings.SetLanguageUseCase
import com.yandex.school.casheye.domain.settings.SetPinUseCase
import com.yandex.school.casheye.domain.settings.SetThemeModeUseCase
import com.yandex.school.casheye.domain.settings.SettingsRepository
import com.yandex.school.casheye.domain.settings.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val mainDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selecting reporting currency saves it and returns to root`() =
        runTest {
            val settings = FakeSettingsRepository()
            val currency = FakeReportingCurrencyRepository()
            val viewModel = settingsViewModel(settings, currency, FakeEditorRepository())

            viewModel.onIntent(SettingsIntent.OpenDestination(SettingsDestination.Currency))
            viewModel.onIntent(SettingsIntent.SelectReportingCurrency(CurrencyCode.USD))
            advanceUntilIdle()

            assertEquals(CurrencyCode.USD, currency.value.value)
            assertEquals(SettingsDestination.Root, viewModel.state.value.destination)
        }

    @Test
    fun `article load preserves cached categories and exposes failure`() =
        runTest {
            val cachedExpense = Category(1, "Food", "🍔", isIncome = false)
            val income = Category(2, "Salary", "💰", isIncome = true)
            val editor =
                FakeEditorRepository(
                    expenseResult = EditorResult.Success(listOf(cachedExpense)),
                    incomeResult = EditorResult.Success(emptyList()),
                )
            val viewModel =
                settingsViewModel(
                    FakeSettingsRepository(),
                    FakeReportingCurrencyRepository(),
                    editor,
                )

            viewModel.onIntent(SettingsIntent.OpenDestination(SettingsDestination.Articles))
            advanceUntilIdle()
            editor.expenseResult = EditorResult.Failure(FinanceFailureReason.Network)
            editor.incomeResult = EditorResult.Success(listOf(income))
            viewModel.onIntent(SettingsIntent.LoadArticles)
            advanceUntilIdle()
            viewModel.onIntent(SettingsIntent.ArticlesQueryChanged("sal"))

            assertEquals(listOf(income), viewModel.state.value.visibleArticles)
            assertEquals(FinanceFailureReason.Network, viewModel.state.value.articlesError)
            assertEquals(listOf(cachedExpense, income), viewModel.state.value.articles)
        }

    @Test
    fun `disabling pin clears it and returns to root`() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel =
                settingsViewModel(
                    settings,
                    FakeReportingCurrencyRepository(),
                    FakeEditorRepository(),
                )

            viewModel.onIntent(SettingsIntent.OpenDestination(SettingsDestination.Pin))
            viewModel.onIntent(SettingsIntent.DisablePin)
            advanceUntilIdle()

            assertEquals(null, settings.lastPin)
            assertEquals(SettingsDestination.Root, viewModel.state.value.destination)
        }

    @Test
    fun `changing biometric preference saves requested state`() =
        runTest {
            val settings = FakeSettingsRepository()
            val viewModel =
                settingsViewModel(
                    settings,
                    FakeReportingCurrencyRepository(),
                    FakeEditorRepository(),
                )

            viewModel.onIntent(SettingsIntent.SetBiometricsEnabled(true))
            advanceUntilIdle()

            assertEquals(true, settings.biometricsEnabled)
            assertEquals(true, viewModel.state.value.settings.security.biometricsEnabled)
        }
}

private fun settingsViewModel(
    settingsRepository: FakeSettingsRepository,
    reportingCurrencyRepository: FakeReportingCurrencyRepository,
    editorRepository: FakeEditorRepository,
): SettingsViewModel =
    SettingsViewModel(
        ObserveSettingsUseCase(settingsRepository),
        ObserveReportingCurrencyUseCase(reportingCurrencyRepository),
        SetReportingCurrencyUseCase(reportingCurrencyRepository),
        SetThemeModeUseCase(settingsRepository),
        SetLanguageUseCase(settingsRepository),
        SetPinUseCase(settingsRepository),
        SetBiometricsEnabledUseCase(settingsRepository),
        GetEditorCategoriesUseCase(editorRepository),
    )

private class FakeSettingsRepository : SettingsRepository {
    val settings = MutableStateFlow(AppSettings())
    var lastPin: CharArray? = "1234".toCharArray()
    var biometricsEnabled = false

    override fun observe() = settings

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override suspend fun setLanguage(language: AppLanguage) = Unit

    override suspend fun setPin(pin: CharArray?) {
        lastPin = pin
    }

    override suspend fun verifyPin(
        pin: CharArray,
        verifier: PinVerifier,
    ) = false

    override suspend fun setBiometricsEnabled(enabled: Boolean) {
        biometricsEnabled = enabled
        settings.value =
            settings.value.copy(
                security = settings.value.security.copy(biometricsEnabled = enabled),
            )
    }
}

private class FakeReportingCurrencyRepository : ReportingCurrencyRepository {
    val value = MutableStateFlow(CurrencyCode.RUB)

    override fun observe() = value

    override suspend fun set(currency: CurrencyCode) {
        value.value = currency
    }
}

private class FakeEditorRepository(
    var expenseResult: EditorResult<List<Category>> = EditorResult.Success(emptyList()),
    var incomeResult: EditorResult<List<Category>> = EditorResult.Success(emptyList()),
) : FinanceEditorRepository {
    override suspend fun getCategories(isIncome: Boolean) = if (isIncome) incomeResult else expenseResult
}
