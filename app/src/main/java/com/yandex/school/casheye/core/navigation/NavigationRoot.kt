package com.yandex.school.casheye.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.feature.accounts.AccountScreen
import com.yandex.school.casheye.feature.expense.ExpenseScreen
import com.yandex.school.casheye.feature.income.IncomeScreen


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

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavigationBar(
                selectedKey = navigationState.topLevelRoute,
                onSelectKey = {
                    navigator.navigate(it)
                }
            )
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
                        AccountScreen()

                    }

                },
            )
        )
    }


}