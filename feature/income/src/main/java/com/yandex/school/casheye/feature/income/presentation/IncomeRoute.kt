package com.yandex.school.casheye.feature.income.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate

@Composable
fun IncomeRoute(
    selectedDate: LocalDate,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: IncomeViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(selectedDate, viewModel) {
        viewModel.onIntent(IncomeIntent.SelectDate(selectedDate))
    }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is IncomeEffect.ShowError -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = "Повторить",
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(IncomeIntent.Retry)
                    }
                }
            }
        }
    }
    IncomeScreen(
        state = state,
        modifier = modifier,
    )
}
