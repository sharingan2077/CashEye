package com.yandex.school.casheye.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.R
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.accounts.presentation.AccountsRoute
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsEntryPoint
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsRoute
import com.yandex.school.casheye.feature.expenses.presentation.ExpensesDatePickerDialog
import com.yandex.school.casheye.feature.expenses.presentation.ExpensesRoute
import com.yandex.school.casheye.feature.income.presentation.IncomeRoute
import java.time.LocalDate

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit = {},
) {
    var selectedDateEpochDay by rememberSaveable {
        mutableLongStateOf(LocalDate.now().toEpochDay())
    }
    val snackbarHostState = remember { SnackbarHostState() }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay)
    val navigationState =
        rememberNavigationState(
            startRoute = Route.Expenses,
            topLevelRoutes = TOP_LEVEL_DESTINATIONS.keys,
        )
    val navigator = remember { Navigator(navigationState) }
    NavigationScaffold(
        modifier = modifier,
        navigationState = navigationState,
        snackbarHostState = snackbarHostState,
        navigator = navigator,
        selectedDate = selectedDate,
        onDateClick = { showDatePicker = true },
        onAddClick = onAddClick,
    )

    if (showDatePicker) {
        ExpensesDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelect = { date ->
                selectedDateEpochDay = date.toEpochDay()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationScaffold(
    navigationState: NavigationState,
    snackbarHostState: SnackbarHostState,
    navigator: Navigator,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
    onDateClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
) {
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()
    val showBars = currentRoute !is Route.Analytics

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBars) {
                BottomNavigationBar(
                    selectedKey = navigationState.topLevelRoute,
                    onSelectKey = navigator::navigate,
                )
            }
        },
        topBar = {
            if (showBars) {
                NavigationTopBar(
                    date = selectedDate,
                    onDateClick = onDateClick,
                    onAnalyticsClick = {
                        navigator.navigate(
                            Route.Analytics(navigationState.topLevelRoute.toAnalyticsEntryPoint()),
                        )
                    },
                )
            } else {
                ArrowTopBar(title = "Аналитика", onBackClick = navigator::goBack)
            }
        },
        floatingActionButton = {
            if (showBars) FloatingButton(onClick = onAddClick)
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
    ) { innerPadding ->
        NavigationContent(
            navigationState = navigationState,
            snackbarHostState = snackbarHostState,
            navigator = navigator,
            selectedDate = selectedDate,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun NavigationContent(
    navigationState: NavigationState,
    snackbarHostState: SnackbarHostState,
    navigator: Navigator,
    selectedDate: LocalDate,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        modifier = modifier.fillMaxSize(),
        onBack = navigator::goBack,
        entries =
            navigationState.toEntries(
                entryProvider {
                    entry<Route.Expenses> {
                        ExpensesRoute(
                            selectedDate = selectedDate,
                            snackbarHostState = snackbarHostState,
                        )
                    }
                    entry<Route.Income> {
                        IncomeRoute(
                            selectedDate = selectedDate,
                            snackbarHostState = snackbarHostState,
                        )
                    }
                    entry<Route.Account> {
                        AccountsRoute(
                            snackbarHostState = snackbarHostState,
                        )
                    }
                    entry<Route.Analytics> { route ->
                        AnalyticsRoute(
                            entryPoint = route.entryPoint.toFeatureEntryPoint(),
                            snackbarHostState = snackbarHostState,
                        )
                    }
                },
            ),
    )
}

private fun androidx.navigation3.runtime.NavKey.toAnalyticsEntryPoint(): AnalyticsRouteEntryPoint =
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

@Composable
private fun FloatingButton(onClick: () -> Unit) {
    FloatingActionButton(onClick = onClick, modifier = Modifier.size(56.dp)) {
        Icon(
            painter = painterResource(R.drawable.plus),
            contentDescription = "Добавить",
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun FloatingButtonPreview() {
    CashEyeTheme(dynamicColor = false) { FloatingButton {} }
}
