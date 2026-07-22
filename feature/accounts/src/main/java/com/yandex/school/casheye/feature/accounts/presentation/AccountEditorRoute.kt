package com.yandex.school.casheye.feature.accounts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
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

    LaunchedEffect(accountId) { viewModel.onIntent(AccountEditorIntent.Open(accountId)) }
    LaunchedEffect(viewModel) { viewModel.effects.collectLatest { currentOnSave() } }
    AccountEditorScreen(state, viewModel::onIntent, onDismiss)
}
