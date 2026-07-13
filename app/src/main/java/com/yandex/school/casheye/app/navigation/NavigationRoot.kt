package com.yandex.school.casheye.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.accounts.presentation.AccountScreen
import com.yandex.school.casheye.feature.accounts.presentation.accountsUiStateMock
import com.yandex.school.casheye.feature.analytics.presentaion.AnalyticsScreen
import com.yandex.school.casheye.feature.expenses.presentation.ExpenseScreen
import com.yandex.school.casheye.feature.income.presentation.IncomeScreen
import java.time.LocalDate


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRoot(
    modifier: Modifier = Modifier
) {

    val navigationState = rememberNavigationState(
        startRoute = Route.Expenses,
        topLevelRoutes = TOP_LEVEL_DESTINATIONS.keys
    )

    val navigator = remember {
        Navigator(navigationState)
    }

    val currentRoute = navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull()

    val showBars = currentRoute != Route.Analytics

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBars) {
                BottomNavigationBar(
                    selectedKey = navigationState.topLevelRoute,
                    onSelectKey = {
                        navigator.navigate(it)
                    }
                )
            }
        },
        topBar = {
            if (showBars) {
                NavigationTopBar(
                    date = LocalDate.of(2026, 6, 12),
                    onAnalyticsClick = { navigator.navigate(Route.Analytics) },

                    )
            } else {
                ArrowTopBar(
                    title = "Аналитика",
                    onBackClick = navigator::goBack
                )
            }
        },
        floatingActionButton = {
            if (showBars) {
                FloatingButton {
                    // TODO: Implement onClick
                }
            }
        }
    ) { innerPadding ->
        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            onBack = navigator::goBack,
            entries = navigationState.toEntries(
                entryProvider {
                    entry<Route.Expenses> {
                        ExpenseScreen()
                    }
                    entry<Route.Income> {
                        IncomeScreen()
                    }
                    entry<Route.Account> {
                        AccountScreen(state = accountsUiStateMock)
                    }
                    entry<Route.Analytics> {

                        AnalyticsScreen()
                    }
                },
            )
        )
    }


}

@Composable
private fun FloatingButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = {
            onClick()
        },
        modifier = Modifier
            .size(56.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Добавить",
            modifier = Modifier
                .size(24.dp)
        )

    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun FloatingButtonPreview() {
    CashEyeTheme(dynamicColor = false) {
        FloatingButton {

        }
    }
}
