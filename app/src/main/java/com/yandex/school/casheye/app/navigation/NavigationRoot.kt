package com.yandex.school.casheye.app.navigation

import androidx.compose.material3.SnackbarDuration
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yandex.school.casheye.R
import com.yandex.school.casheye.app.navigation.chrome.NavigationScaffold
import com.yandex.school.casheye.app.navigation.chrome.TOP_LEVEL_DESTINATIONS
import com.yandex.school.casheye.app.navigation.editor.EditorOverlayHost
import com.yandex.school.casheye.app.navigation.editor.EditorTarget
import com.yandex.school.casheye.core.designsystem.component.datepicker.PastOrPresentDatePickerDialog
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

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
    val offlineMessage = stringResource(R.string.network_offline_message)

    LaunchedEffect(isOnline) {
        val wasOnline = previousOnline
        previousOnline = isOnline
        if (!isOnline && wasOnline != false) {
            snackbarHostState.showSnackbar(
                message = offlineMessage,
                withDismissAction = true,
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

    NavigationScaffold(
        modifier = modifier,
        navigationState = navigationState,
        snackbarHostState = snackbarHostState,
        navigator = navigator,
        selectedDate = selectedDate,
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
}
