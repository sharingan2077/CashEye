package com.yandex.school.casheye.feature.accounts.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun AccountsRoute(
    modifier: Modifier = Modifier,
    viewModel: AccountsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountScreen(state = state, modifier = modifier)
}
