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
import com.yandex.school.casheye.feature.accounts.presentation.AccountScreen
import com.yandex.school.casheye.feature.accounts.presentation.accountsUiStateMock
import com.yandex.school.casheye.feature.expenses.presentation.ExpenseScreen
import com.yandex.school.casheye.feature.expenses.presentation.expensesUiStateMock
import com.yandex.school.casheye.feature.income.presentation.IncomeScreen
import com.yandex.school.casheye.feature.income.presentation.incomeUiStateMock
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationRoot(modifier: Modifier = Modifier) {
    val navigationState =
        rememberNavigationState(
            startRoute = Route.Expenses,
            topLevelRoutes = TOP_LEVEL_DESTINATIONS.keys,
        )

    val navigator =
        remember {
            Navigator(navigationState)
        }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            BottomNavigationBar(
                selectedKey = navigationState.topLevelRoute,
                onSelectKey = {
                    navigator.navigate(it)
                },
            )
        },
        topBar = {
            NavigationTopBar(LocalDate.of(2026, 6, 12))
        },
        floatingActionButton = {
            FloatingButton {
                // TODO: Implement onCLick
            }
        },
    ) { innerPadding ->
        NavDisplay(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            onBack = navigator::goBack,
            entries =
                navigationState.toEntries(
                    entryProvider {
                        entry<Route.Expenses> {
                            ExpenseScreen(state = expensesUiStateMock)
                        }
                        entry<Route.Income> {
                            IncomeScreen(state = incomeUiStateMock)
                        }
                        entry<Route.Account> {
                            AccountScreen(state = accountsUiStateMock)
                        }
                    },
                ),
        )
    }
}

@Composable
private fun FloatingButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = {
            onClick()
        },
        modifier =
            Modifier
                .size(56.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.plus),
            contentDescription = "Добавить",
            modifier =
                Modifier
                    .size(24.dp),
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
