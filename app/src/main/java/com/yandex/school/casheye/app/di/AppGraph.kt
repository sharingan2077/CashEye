package com.yandex.school.casheye.app.di

import com.yandex.school.casheye.data.expenses.di.ExpensesNetworkBindings
import com.yandex.school.casheye.data.expenses.di.ExpensesRepositoryBindings
import com.yandex.school.casheye.data.expenses.di.NetworkConfig
import com.yandex.school.casheye.feature.accounts.di.AccountsViewModelBindings
import com.yandex.school.casheye.feature.analytics.di.AnalyticsViewModelBindings
import com.yandex.school.casheye.feature.expenses.di.ExpensesUseCaseBindings
import com.yandex.school.casheye.feature.expenses.di.ExpensesViewModelBindings
import com.yandex.school.casheye.feature.income.di.IncomeViewModelBindings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [
        ExpensesNetworkBindings::class,
        ExpensesRepositoryBindings::class,
        ExpensesUseCaseBindings::class,
        ExpensesViewModelBindings::class,
        IncomeViewModelBindings::class,
        AccountsViewModelBindings::class,
        AnalyticsViewModelBindings::class,
        AppViewModelBindings::class,
    ],
)
interface AppGraph : ViewModelGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides networkConfig: NetworkConfig,
        ): AppGraph
    }
}
