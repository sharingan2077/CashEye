package com.yandex.school.casheye.feature.expenses.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun AddExpenseRoute(
    transactionId: Int?,
    defaultDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    viewModel: AddExpenseViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSave by rememberUpdatedState(onSave)

    LaunchedEffect(transactionId, defaultDate) {
        viewModel.onIntent(AddExpenseIntent.Open(transactionId, defaultDate))
    }
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { currentOnSave }
    }
    AddExpenseScreen(state, viewModel::onIntent, onDismiss)
}
