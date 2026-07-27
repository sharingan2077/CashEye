package com.yandex.school.casheye.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.app.navigation.chrome.APP_CHROME_ANIMATION_DURATION_MILLIS
import com.yandex.school.casheye.feature.accounts.presentation.AccountsRoute
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsEntryPoint
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsRoute
import com.yandex.school.casheye.feature.expenses.presentation.ExpensesRoute
import com.yandex.school.casheye.feature.income.presentation.IncomeRoute
import java.time.LocalDate

@Composable
internal fun NavigationContent(
    navigationState: NavigationState,
    snackbarHostState: SnackbarHostState,
    navigator: Navigator,
    selectedDate: LocalDate,
    networkRecoveryRefresh: NetworkRecoveryRefresh?,
    onNetworkRecoveryRefreshConsumed: (Long) -> Unit,
    onEditExpense: (Int) -> Unit,
    onEditIncome: (Int) -> Unit,
    onEditAccount: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        modifier = modifier.fillMaxSize(),
        onBack = navigator::goBack,
        transitionSpec = {
            fadeIn(tween(APP_CHROME_ANIMATION_DURATION_MILLIS)) togetherWith
                fadeOut(tween(APP_CHROME_ANIMATION_DURATION_MILLIS))
        },
        popTransitionSpec = {
            fadeIn(tween(APP_CHROME_ANIMATION_DURATION_MILLIS)) togetherWith
                fadeOut(tween(APP_CHROME_ANIMATION_DURATION_MILLIS))
        },
        entries =
            navigationState.toEntries(
                entryProvider {
                    entry<Route.Expenses> {
                        ExpensesRoute(
                            selectedDate = selectedDate,
                            snackbarHostState = snackbarHostState,
                            onTransactionClick = onEditExpense,
                            networkRecoveryRefreshId =
                                networkRecoveryRefresh?.takeIf { it.route == Route.Expenses }?.id,
                            onNetworkRecoveryRefreshConsumed = onNetworkRecoveryRefreshConsumed,
                        )
                    }
                    entry<Route.Income> {
                        IncomeRoute(
                            selectedDate = selectedDate,
                            snackbarHostState = snackbarHostState,
                            onTransactionClick = onEditIncome,
                            networkRecoveryRefreshId =
                                networkRecoveryRefresh?.takeIf { it.route == Route.Income }?.id,
                            onNetworkRecoveryRefreshConsumed = onNetworkRecoveryRefreshConsumed,
                        )
                    }
                    entry<Route.Account> {
                        AccountsRoute(
                            snackbarHostState = snackbarHostState,
                            onAccountClick = onEditAccount,
                            networkRecoveryRefreshId =
                                networkRecoveryRefresh?.takeIf { it.route == Route.Account }?.id,
                            onNetworkRecoveryRefreshConsumed = onNetworkRecoveryRefreshConsumed,
                        )
                    }
                    entry<Route.Analytics> { route ->
                        AnalyticsRoute(
                            entryPoint = route.entryPoint.toFeatureEntryPoint(),
                            snackbarHostState = snackbarHostState,
                            networkRecoveryRefreshId =
                                networkRecoveryRefresh?.takeIf { it.route == route }?.id,
                            onNetworkRecoveryRefreshConsumed = onNetworkRecoveryRefreshConsumed,
                        )
                    }
                },
            ),
    )
}

internal fun NavKey.toAnalyticsEntryPoint(): AnalyticsRouteEntryPoint =
    when (this) {
        Route.Income -> AnalyticsRouteEntryPoint.Income
        Route.Account -> AnalyticsRouteEntryPoint.Accounts
        else -> AnalyticsRouteEntryPoint.Expenses
    }

private fun AnalyticsRouteEntryPoint.toFeatureEntryPoint(): AnalyticsEntryPoint =
    when (this) {
        AnalyticsRouteEntryPoint.Expenses -> AnalyticsEntryPoint.Expenses
        AnalyticsRouteEntryPoint.Income -> AnalyticsEntryPoint.Income
        AnalyticsRouteEntryPoint.Accounts -> AnalyticsEntryPoint.Accounts
    }
