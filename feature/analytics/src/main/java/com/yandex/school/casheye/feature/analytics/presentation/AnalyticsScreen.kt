package com.yandex.school.casheye.feature.analytics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.yandex.school.casheye.core.designsystem.component.DelayedCircularProgressIndicator
import com.yandex.school.casheye.core.designsystem.component.ErrorState
import com.yandex.school.casheye.core.designsystem.component.PullToRefreshContainer
import com.yandex.school.casheye.feature.analytics.R
import com.yandex.school.casheye.feature.analytics.presentation.sheet.AnalyticsBottomSheet

@Composable
fun AnalyticsScreen(
    state: AnalyticsUiState,
    onIntent: (AnalyticsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PullToRefreshContainer(
        isRefreshing = state.isRefreshing,
        onRefresh = { onIntent(AnalyticsIntent.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
        ) {
            when (state) {
                is AnalyticsUiState.Loading -> {
                    DelayedCircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is AnalyticsUiState.Content -> {
                    AnalyticsContent(state, onIntent)
                }

                is AnalyticsUiState.Empty -> {
                    AnalyticsEmpty(state, onIntent)
                }

                is AnalyticsUiState.Error -> {
                    ErrorState(
                        type = state.reason.toErrorStateType(),
                        onRetry = { onIntent(AnalyticsIntent.Retry) },
                        retryLabel = stringResource(R.string.retry),
                    )
                }
            }
        }
    }
    AnalyticsBottomSheet(state = state, onIntent = onIntent)
}
