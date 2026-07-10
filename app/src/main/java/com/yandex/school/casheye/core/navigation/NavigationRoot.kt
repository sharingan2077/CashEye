package com.yandex.school.casheye.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.feature.accounts.AccountScreen
import com.yandex.school.casheye.feature.expense.ExpenseScreen
import com.yandex.school.casheye.feature.income.IncomeScreen
import com.yandex.school.casheye.ui.theme.CashEyeTheme


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

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavigationBar(
                selectedKey = navigationState.topLevelRoute,
                onSelectKey = {
                    navigator.navigate(it)
                }
            )
        },
        topBar = { NavigationTopBar() },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationTopBar() {
    TopAppBar(
        title = {},
        actions = {},
    )
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun NavigationTopBarPreview() {
    CashEyeTheme(dynamicColor = false) {
        NavigationTopBar()
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 892)
@Composable
private fun NavigationRootPreview() {
    CashEyeTheme(dynamicColor = false) {
        Surface {
            NavigationRoot()
        }
    }
}
