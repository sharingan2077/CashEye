package com.yandex.school.casheye.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.accounts.editor.presentation.AddAccountRoute
import com.yandex.school.casheye.feature.accounts.presentation.AccountsScreen
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsScreen
import com.yandex.school.casheye.feature.transactions.editor.presentation.AddTransactionRoute
import com.yandex.school.casheye.feature.transactions.editor.presentation.TransactionType
import com.yandex.school.casheye.feature.transactions.expenses.presentation.ExpensesScreen
import com.yandex.school.casheye.feature.transactions.income.presentation.IncomeScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class AddSheetType {
    EXPENSE,
    INCOME,
    ACCOUNT
}

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

    val sheetForCurrentRoute: AddSheetType? = when (currentRoute) {
        is Route.Expenses -> AddSheetType.EXPENSE
        is Route.Income -> AddSheetType.INCOME
        is Route.Accounts -> AddSheetType.ACCOUNT
        else -> null
    }

    var openedSheet by rememberSaveable {
        mutableStateOf<AddSheetType?>(null)
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val scope = rememberCoroutineScope()

    val closeSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
            openedSheet = null
        }
    }



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
            if (showBars && sheetForCurrentRoute != null) {
                FloatingButton {
                    openedSheet = sheetForCurrentRoute
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
                        ExpensesScreen()
                    }
                    entry<Route.Income> {
                        IncomeScreen()
                    }
                    entry<Route.Accounts> {
                        AccountsScreen()
                    }
                    entry<Route.Analytics> {
                        AnalyticsScreen()
                    }
                },
            )
        )
    }

    openedSheet?.let { type ->
        ModalBottomSheet(
            onDismissRequest = {
                openedSheet = null
            },
            sheetState = sheetState,
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 24.dp
            ),
            containerColor = Color.White,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            when (type) {
                AddSheetType.EXPENSE -> {
                    AddTransactionRoute(
                        type = TransactionType.EXPENSE,
                        onSaved = closeSheet,
                    )
                }

                AddSheetType.INCOME -> {
                    AddTransactionRoute(
                        type = TransactionType.INCOME,
                        onSaved = closeSheet,
                    )
                }

                AddSheetType.ACCOUNT -> {
                    AddAccountRoute(
                        onSaved = closeSheet
                    )
                }
            }
        }

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
