package com.yandex.school.casheye.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.R
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.accounts.presentation.AccountsRoute
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsRoute
import com.yandex.school.casheye.feature.expenses.presentation.ExpensesRoute
import com.yandex.school.casheye.feature.income.presentation.IncomeRoute
import java.time.LocalDate

@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit = {},
) {
    val navigationState =
        rememberNavigationState(
            startRoute = Route.Expenses,
            topLevelRoutes = TOP_LEVEL_DESTINATIONS.keys,
        )
    val navigator = remember { Navigator(navigationState) }

    NavigationScaffold(
        modifier = modifier,
        navigationState = navigationState,
        navigator = navigator,
        onAddClick = onAddClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationScaffold(
    navigationState: NavigationState,
    navigator: Navigator,
    modifier: Modifier = Modifier,
    onAddClick: () -> Unit = {},
) {
    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()
    val showBars = currentRoute != Route.Analytics

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
                    date = navigationDate,
                    onAnalyticsClick = { navigator.navigate(Route.Analytics) },
                )
            } else {
                ArrowTopBar(title = "Аналитика", onBackClick = navigator::goBack)
            }
        },
        floatingActionButton = {
            if (showBars) FloatingButton(onClick = onAddClick)
        },
    ) { innerPadding ->
        NavigationContent(
            navigationState = navigationState,
            navigator = navigator,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun NavigationContent(
    navigationState: NavigationState,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    NavDisplay(
        modifier = modifier.fillMaxSize(),
        onBack = navigator::goBack,
        entries =
            navigationState.toEntries(
                entryProvider {
                    entry<Route.Expenses> { ExpensesRoute() }
                    entry<Route.Income> { IncomeRoute() }
                    entry<Route.Account> { AccountsRoute() }
                    entry<Route.Analytics> { AnalyticsRoute() }
                },
            ),
    )
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

private val navigationDate: LocalDate = LocalDate.now()
