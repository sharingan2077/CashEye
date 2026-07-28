package com.yandex.school.casheye.feature.settings.presentation

import com.yandex.school.casheye.domain.settings.AppSettings

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val destination: SettingsDestination = SettingsDestination.Root,
)

sealed interface SettingsIntent {
    data class OpenDestination(
        val destination: SettingsDestination,
    ) : SettingsIntent

    data object BackToRoot : SettingsIntent

    data object Reset : SettingsIntent
}

sealed interface SettingsDestination {
    data object Root : SettingsDestination

    data object Currency : SettingsDestination

    data object Articles : SettingsDestination

    data object Appearance : SettingsDestination

    data object Language : SettingsDestination

    data object Pin : SettingsDestination

    data object Biometrics : SettingsDestination
}
