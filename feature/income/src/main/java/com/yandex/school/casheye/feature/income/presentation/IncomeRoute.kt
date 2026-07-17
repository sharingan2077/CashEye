package com.yandex.school.casheye.feature.income.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun IncomeRoute(
    modifier: Modifier = Modifier,
    viewModel: IncomeViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    IncomeScreen(state = state, modifier = modifier)
}
