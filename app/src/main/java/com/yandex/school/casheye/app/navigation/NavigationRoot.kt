package com.yandex.school.casheye.app.navigation

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.R
import com.yandex.school.casheye.app.navigation.chrome.NavigationScaffold
import com.yandex.school.casheye.app.navigation.chrome.TOP_LEVEL_DESTINATIONS
import com.yandex.school.casheye.app.navigation.editor.EditorOverlayHost
import com.yandex.school.casheye.app.navigation.editor.EditorTarget
import com.yandex.school.casheye.core.designsystem.component.datepicker.PastOrPresentDatePickerDialog
import com.yandex.school.casheye.feature.settings.presentation.SettingsSheetRoute
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

private object SettingsSheetSessionState {
    var isVisible: Boolean? = null
}

@Composable
fun NavigationRoot(
    networkStatus: StateFlow<Boolean>,
    biometricsAvailable: Boolean,
    onRequestBiometricAuthentication: (onResult: (Boolean) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDateEpochDay by rememberSaveable {
        mutableLongStateOf(LocalDate.now().toEpochDay())
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline by networkStatus.collectAsStateWithLifecycle()
    var previousOnline by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var networkRecoveryRefresh by remember { mutableStateOf<NetworkRecoveryRefresh?>(null) }
    var nextNetworkRecoveryRefreshId by remember { mutableLongStateOf(0L) }
    val offlineMessage = stringResource(R.string.network_offline_message)

    val navigationState =
        rememberNavigationState(
            startRoute = Route.Expenses,
            topLevelRoutes = TOP_LEVEL_DESTINATIONS.keys,
        )
    val navigator = remember { Navigator(navigationState) }

    LaunchedEffect(isOnline) {
        val wasOnline = previousOnline
        previousOnline = isOnline
        if (!isOnline && wasOnline != false) {
            snackbarHostState.showSnackbar(
                message = offlineMessage,
                withDismissAction = true,
                duration = SnackbarDuration.Long,
            )
        } else if (isOnline && wasOnline == false) {
            val route =
                navigationState.backStacks[navigationState.topLevelRoute]?.lastOrNull() as? Route
            if (route != null) {
                nextNetworkRecoveryRefreshId += 1
                networkRecoveryRefresh =
                    NetworkRecoveryRefresh(id = nextNetworkRecoveryRefreshId, route = route)
            }
        }
    }

    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val selectedDate = LocalDate.ofEpochDay(selectedDateEpochDay).coerceAtMost(LocalDate.now())
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var showSettings by rememberSaveable {
        mutableStateOf(SettingsSheetSessionState.isVisible ?: false)
    }
    SideEffect { SettingsSheetSessionState.isVisible = showSettings }

    NavigationScaffold(
        modifier = modifier,
        navigationState = navigationState,
        snackbarHostState = snackbarHostState,
        navigator = navigator,
        selectedDate = selectedDate,
        networkRecoveryRefresh = networkRecoveryRefresh,
        onNetworkRecoveryRefresh = { id ->
            if (networkRecoveryRefresh?.id == id) {
                networkRecoveryRefresh = null
            }
        },
        onDateClick = { showDatePicker = true },
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
        onSettingsClick = { showSettings = true },
    )

    if (showDatePicker) {
        PastOrPresentDatePickerDialog(
            selectedDate = selectedDate,
            onDateSelect = { date ->
                selectedDateEpochDay = date.toEpochDay()
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }

    EditorOverlayHost(
        target = editorTarget,
        selectedDate = selectedDate,
        onDismiss = { editorTarget = null },
    )

    if (showSettings) {
        SettingsSheetRoute(
            onDismiss = { showSettings = false },
            biometricsAvailable = biometricsAvailable,
            onRequestBiometricEnable = onRequestBiometricAuthentication,
        )
    }
}

internal data class NetworkRecoveryRefresh(
    val id: Long,
    val route: Route,
)
