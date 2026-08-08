package com.yandex.school.casheye.app.di

import android.content.Context
import com.yandex.school.casheye.data.finance.di.FinanceNetworkBindings
import com.yandex.school.casheye.data.finance.di.FinanceRepositoryBindings
import com.yandex.school.casheye.data.finance.di.FinanceSyncBindings
import com.yandex.school.casheye.data.finance.di.FinanceUseCaseBindings
import com.yandex.school.casheye.data.finance.di.NetworkConfig
import com.yandex.school.casheye.data.finance.network.NetworkMonitor
import com.yandex.school.casheye.data.finance.sync.ExchangeRateRefreshScheduler
import com.yandex.school.casheye.data.finance.sync.ExchangeRateRefresher
import com.yandex.school.casheye.data.finance.sync.FinanceSyncScheduler
import com.yandex.school.casheye.data.finance.sync.FinanceSyncer
import com.yandex.school.casheye.data.settings.di.SettingsRepositoryBindings
import com.yandex.school.casheye.domain.settings.ObserveSettingsUseCase
import com.yandex.school.casheye.domain.settings.VerifyPinUseCase
import com.yandex.school.casheye.feature.accounts.di.AccountsViewModelBindings
import com.yandex.school.casheye.feature.analytics.di.AnalyticsViewModelBindings
import com.yandex.school.casheye.feature.expenses.di.ExpensesViewModelBindings
import com.yandex.school.casheye.feature.income.di.IncomeViewModelBindings
import com.yandex.school.casheye.feature.settings.di.SettingsViewModelBindings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [
        FinanceNetworkBindings::class,
        FinanceRepositoryBindings::class,
        FinanceSyncBindings::class,
        FinanceUseCaseBindings::class,
        SettingsRepositoryBindings::class,
        ExpensesViewModelBindings::class,
        IncomeViewModelBindings::class,
        AccountsViewModelBindings::class,
        AnalyticsViewModelBindings::class,
        SettingsViewModelBindings::class,
        AppViewModelBindings::class,
    ],
)
interface AppGraph : ViewModelGraph {
    val networkMonitor: NetworkMonitor

    val observeSettings: ObserveSettingsUseCase

    val verifyPin: VerifyPinUseCase

    val financeSyncer: FinanceSyncer

    val financeSyncScheduler: FinanceSyncScheduler

    val exchangeRateRefresher: ExchangeRateRefresher

    val exchangeRateRefreshScheduler: ExchangeRateRefreshScheduler

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides networkConfig: NetworkConfig,
            @Provides context: Context,
        ): AppGraph
    }
}
