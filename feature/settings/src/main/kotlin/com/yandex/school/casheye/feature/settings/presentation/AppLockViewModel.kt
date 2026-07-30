package com.yandex.school.casheye.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.domain.settings.VerifyPinUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppLockVerificationState {
    Idle,
    Verifying,
    Success,
    Error,
}

data class AppLockUiState(
    val verification: AppLockVerificationState = AppLockVerificationState.Idle,
)

sealed interface AppLockIntent {
    data class SubmitPin(
        val pin: CharArray,
        val verifier: PinVerifier,
    ) : AppLockIntent

    data object SuccessAnimationFinished : AppLockIntent
    data object ErrorAnimationFinished : AppLockIntent
}

@Inject
class AppLockViewModel(
    private val verifyPin: VerifyPinUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(AppLockUiState())
    val state = _state.asStateFlow()

    fun onIntent(intent: AppLockIntent) {
        when (intent) {
            is AppLockIntent.SubmitPin -> verify(intent.pin, intent.verifier)
            AppLockIntent.SuccessAnimationFinished -> _state.value = AppLockUiState()
            AppLockIntent.ErrorAnimationFinished -> _state.value = AppLockUiState()
        }
    }

    private fun verify(pin: CharArray, verifier: PinVerifier) {
        if (_state.value.verification == AppLockVerificationState.Verifying) return
        viewModelScope.launch {
            _state.value = AppLockUiState(AppLockVerificationState.Verifying)
            val isCorrect = try {
                verifyPin(pin, verifier)
            } finally {
                pin.fill('\u0000')
            }
            _state.value = AppLockUiState(
                if (isCorrect) AppLockVerificationState.Success else AppLockVerificationState.Error,
            )
        }
    }
}
