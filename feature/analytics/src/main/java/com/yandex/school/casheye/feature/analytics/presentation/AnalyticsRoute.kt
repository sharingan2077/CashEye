package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.zacsweers.metrox.viewmodel.metroViewModel

@Composable
fun AnalyticsRoute(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AnalyticsScreen(state = state, modifier = modifier)
}
