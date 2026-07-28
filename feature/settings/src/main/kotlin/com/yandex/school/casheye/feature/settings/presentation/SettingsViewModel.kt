package com.yandex.school.casheye.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.settings.ObserveSettingsUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Inject
class SettingsViewModel(
    observeSettings: ObserveSettingsUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeSettings().collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }
    }

    fun onIntent(intent: SettingsIntent) {
        _state.value =
            when (intent) {
                is SettingsIntent.OpenDestination -> _state.value.copy(destination = intent.destination)
                SettingsIntent.BackToRoot,
                SettingsIntent.Reset,
                -> _state.value.copy(destination = SettingsDestination.Root)
            }
    }
}
