package com.yandex.school.casheye.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yandex.school.casheye.R
import com.yandex.school.casheye.core.designsystem.theme.CashEyeTheme
import com.yandex.school.casheye.feature.accounts.presentation.AccountEditorRoute
import com.yandex.school.casheye.feature.accounts.presentation.AccountsRoute
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsEntryPoint
import com.yandex.school.casheye.feature.analytics.presentation.AnalyticsRoute
import com.yandex.school.casheye.feature.expenses.presentation.AddExpenseRoute
import com.yandex.school.casheye.feature.expenses.presentation.ExpensesDatePickerDialog
import com.yandex.school.casheye.feature.expenses.presentation.ExpensesRoute
import com.yandex.school.casheye.feature.income.presentation.AddIncomeRoute
import com.yandex.school.casheye.feature.income.presentation.IncomeRoute
import java.time.LocalDate
import kotlinx.coroutines.flow.StateFlow

@Composable
fun NavigationRoot(
    networkStatus: StateFlow<Boolean>,
    modifier: Modifier = Modifier,
) {
    var selectedDateEpochDay by rememberSaveable {
        mutableLongStateOf(LocalDate.now().toEpochDay())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline by networkStatus.collectAsStateWithLifecycle()
    var previousOnline by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var networkRefreshKey by rememberSaveable { mutableLongStateOf(0) }
    val offlineMessage = stringResource(R.string.network_offline_message)
    val restoredMessage = stringResource(R.string.network_restored_message)

    LaunchedEffect(isOnline) {
        val wasOnline = previousOnline
        previousOnline = isOnline
        if (isOnline && wasOnline == false) {
            networkRefreshKey++
        }
        val message =
            when {
                !isOnline && wasOnline != false -> offlineMessage
                isOnline && wasOnline == false -> restoredMessage
                else -> null
            }
        if (message != null) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
            )
        }
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay).coerceAtMost(LocalDate.now())
    val navigationState =
        rememberNavigationState(
            startRoute = Route.Expenses,
            topLevelRoutes = TOP_LEVEL_DESTINATIONS.keys,
        )
    val navigator = remember { Navigator(navigationState) }
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var expensesRefreshKey by rememberSaveable { mutableLongStateOf(0) }
    var incomeRefreshKey by rememberSaveable { mutableLongStateOf(0) }
    var accountsRefreshKey by rememberSaveable { mutableLongStateOf(0) }
    NavigationScaffold(
        modifier = modifier,
        navigationState = navigationState,
        snackbarHostState = snackbarHostState,
        navigator = navigator,
        selectedDate = selectedDate,
        onDateClick = { showDatePicker = true },
        expensesRefreshKey = expensesRefreshKey,
        incomeRefreshKey = incomeRefreshKey,
        accountsRefreshKey = accountsRefreshKey,
        networkRefreshKey = networkRefreshKey,
        onEditExpense = { editorTarget = EditorTarget.Expense(it) },
        onEditIncome = { editorTarget = EditorTarget.Income(it) },
        onEditAccount = { editorTarget = EditorTarget.Account(it) },
        onAddClick = {
            editorTarget =
                when (navigationState.topLevelRoute) {
                    Route.Income -> EditorTarget.Income(null)
                    Route.Account -> EditorTarget.Account(null)
                    else -> EditorTarget.Expense(null)
                }
        },
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

    when (val target = editorTarget) {
        is EditorTarget.Expense -> {
            AddExpenseRoute(
                transactionId = target.id,
                defaultDate = selectedDate,
                onDismiss = { editorTarget = null },
                onSave = {
                    expensesRefreshKey++
                    editorTarget = null
                },
            )
        }

        is EditorTarget.Income -> {
            AddIncomeRoute(
                transactionId = target.id,
                defaultDate = selectedDate,
                onDismiss = { editorTarget = null },
                onSave = {
                    incomeRefreshKey++
                    editorTarget = null
                },
            )
        }

        is EditorTarget.Account -> {
            AccountEditorRoute(
                accountId = target.id,
                onDismiss = { editorTarget = null },
                onSave = {
                    accountsRefreshKey++
                    editorTarget = null
                },
            )
        }

        null -> {
            Unit
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationScaffold(
    navigationState: NavigationState,
    snackbarHostState: SnackbarHostState,
    navigator: Navigator,
    selectedDate: LocalDate,
    expensesRefreshKey: Long,
    incomeRefreshKey: Long,
    accountsRefreshKey: Long,
    networkRefreshKey: Long,
    onEditExpense: (Int) -> Unit,
    onEditIncome: (Int) -> Unit,
    onEditAccount: (Int) -> Unit,
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
                ArrowTopBar(title = stringResource(R.string.analytics_title), onBackClick = navigator::goBack)
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
            expensesRefreshKey = expensesRefreshKey,
            incomeRefreshKey = incomeRefreshKey,
            accountsRefreshKey = accountsRefreshKey,
            networkRefreshKey = networkRefreshKey,
            onEditExpense = onEditExpense,
            onEditIncome = onEditIncome,
            onEditAccount = onEditAccount,
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
    expensesRefreshKey: Long,
    incomeRefreshKey: Long,
    accountsRefreshKey: Long,
    networkRefreshKey: Long,
    onEditExpense: (Int) -> Unit,
    onEditIncome: (Int) -> Unit,
    onEditAccount: (Int) -> Unit,
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
                            refreshKey = expensesRefreshKey + networkRefreshKey,
                            onTransactionClick = onEditExpense,
                        )
                    }
                    entry<Route.Income> {
                        IncomeRoute(
                            selectedDate = selectedDate,
                            snackbarHostState = snackbarHostState,
                            refreshKey = incomeRefreshKey + networkRefreshKey,
                            onTransactionClick = onEditIncome,
                        )
                    }
                    entry<Route.Account> {
                        AccountsRoute(
                            snackbarHostState = snackbarHostState,
                            refreshKey = accountsRefreshKey + networkRefreshKey,
                            onAccountClick = onEditAccount,
                        )
                    }
                    entry<Route.Analytics> { route ->
                        AnalyticsRoute(
                            entryPoint = route.entryPoint.toFeatureEntryPoint(),
                            snackbarHostState = snackbarHostState,
                            refreshKey = networkRefreshKey,
                        )
                    }
                },
            ),
    )
}

private sealed interface EditorTarget {
    data class Expense(
        val id: Int?,
    ) : EditorTarget

    data class Income(
        val id: Int?,
    ) : EditorTarget

    data class Account(
        val id: Int?,
    ) : EditorTarget
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
            contentDescription = stringResource(R.string.content_description_add),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
private fun FloatingButtonPreview() {
    CashEyeTheme(dynamicColor = false) { FloatingButton {} }
}
