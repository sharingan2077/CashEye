package com.yandex.school.casheye.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.R
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
        title = {
            Box(
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = CircleShape
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = "Календарь"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "12 июня",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        actions = {
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
            Icon(
                painter = painterResource(R.drawable.analytics),
                contentDescription = "Аналитика"
            )
            }
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Фильтры"
            )
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun NavigationTopBarPreview() {
    CashEyeTheme(dynamicColor = false) {
        NavigationTopBar()
    }
}