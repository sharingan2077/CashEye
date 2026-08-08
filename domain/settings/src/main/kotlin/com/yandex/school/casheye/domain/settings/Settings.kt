package com.yandex.school.casheye.domain.settings

import kotlinx.coroutines.flow.Flow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AppLanguage(
    val languageTag: String?,
) {
    SYSTEM(null),
    RUSSIAN("ru"),
    ENGLISH("en"),
    GERMAN("de"),
    FRENCH("fr"),
    SPANISH("es"),
}

data class PinVerifier(
    val salt: String,
    val hash: String,
)

data class SecuritySettings(
    val pinVerifier: PinVerifier? = null,
    val biometricsEnabled: Boolean = false,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val security: SecuritySettings = SecuritySettings(),
)

interface SettingsRepository {
    fun observe(): Flow<AppSettings>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setPin(pin: CharArray?)

    suspend fun verifyPin(
        pin: CharArray,
        verifier: PinVerifier,
    ): Boolean

    suspend fun setBiometricsEnabled(enabled: Boolean)
}
