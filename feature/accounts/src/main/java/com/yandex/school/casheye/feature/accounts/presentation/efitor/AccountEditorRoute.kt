package com.yandex.school.casheye.feature.accounts.presentation.efitor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AccountEditorRoute(
    accountId: Int?,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    viewModel: AccountEditorViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnSave by rememberUpdatedState(onSave)
    var currencySelectionRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(accountId) { viewModel.onIntent(AccountEditorIntent.Open(accountId)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                AccountEditorEffect.Saved -> currentOnSave()
                AccountEditorEffect.OpenCurrencySelector -> currencySelectionRequest += 1
            }
        }
    }
    AccountEditorScreen(state, viewModel::onIntent, currencySelectionRequest, onDismiss)
}
