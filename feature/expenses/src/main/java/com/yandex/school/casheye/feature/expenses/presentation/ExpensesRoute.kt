package com.yandex.school.casheye.feature.expenses.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ExpensesRoute(
    modifier: Modifier = Modifier,
    viewModel: ExpensesViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ExpensesEffect.ShowError -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = "Повторить",
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(ExpensesIntent.Retry)
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ExpenseScreen(
            state = state,
            onRetry = { viewModel.onIntent(ExpensesIntent.Retry) },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
