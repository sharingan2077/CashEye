package com.yandex.school.casheye.domain.settings

import kotlinx.coroutines.flow.Flow

class ObserveSettingsUseCase(
    private val repository: SettingsRepository,
) {
    operator fun invoke(): Flow<AppSettings> = repository.observe()
}

class SetThemeModeUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(mode: ThemeMode) = repository.setThemeMode(mode)
}

class SetLanguageUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(language: AppLanguage) = repository.setLanguage(language)
}

class SetPinVerifierUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(verifier: PinVerifier?) = repository.setPinVerifier(verifier)
}

class SetPinUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(pin: CharArray?) = repository.setPin(pin)
}

class VerifyPinUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(
        pin: CharArray,
        verifier: PinVerifier,
    ): Boolean = repository.verifyPin(pin, verifier)
}

class SetBiometricsEnabledUseCase(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setBiometricsEnabled(enabled)
}
