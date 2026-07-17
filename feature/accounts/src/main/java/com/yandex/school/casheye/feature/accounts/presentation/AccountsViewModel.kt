package com.yandex.school.casheye.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Inject
class AccountsViewModel : ViewModel() {
    private val _state = MutableStateFlow(accountsUiStateMock)
    val state: StateFlow<AccountsUiState> = _state.asStateFlow()
}
