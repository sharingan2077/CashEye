package com.yandex.school.casheye.feature.accounts.presentation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AccountsRoute(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is AccountsEffect.ShowError -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = "Повторить",
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(AccountsIntent.Retry)
                    }
                }
            }
        }
    }

    AccountsScreen(
        state = state,
        modifier = modifier,
    )
}
