package com.yandex.school.casheye.feature.income.presentation

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
class IncomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(incomeUiStateMock)
    val state: StateFlow<IncomeUiState> = _state.asStateFlow()
}
