package com.yandex.school.casheye.feature.analytics.presentation

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
class AnalyticsViewModel : ViewModel() {
    private val _state = MutableStateFlow(analyticsUiStateMock)
    val state: StateFlow<AnalyticsUiState> = _state.asStateFlow()
}
