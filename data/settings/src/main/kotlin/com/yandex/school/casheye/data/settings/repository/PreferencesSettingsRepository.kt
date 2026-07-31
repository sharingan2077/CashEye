package com.yandex.school.casheye.data.settings.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yandex.school.casheye.domain.settings.AppLanguage
import com.yandex.school.casheye.domain.settings.AppSettings
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.domain.settings.SettingsRepository
import com.yandex.school.casheye.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import java.io.IOException

/**
 * Combines non-secret appearance preferences with encrypted security settings behind one domain
 * repository, removing legacy plaintext verifier entries on first observation.
 */
internal class PreferencesSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val securityStore: EncryptedSecuritySettingsStore,
) : SettingsRepository {
    override fun observe(): Flow<AppSettings> =
        combine(
            dataStore.data
                .onStart { clearLegacySecuritySettings() }
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map { preferences ->
                    AppSettings(
                        themeMode = preferences[THEME_MODE_KEY].toThemeMode(),
                        language = preferences[LANGUAGE_KEY].toLanguage(),
                    )
                },
            securityStore.observe(),
        ) { applicationSettings, security -> applicationSettings.copy(security = security) }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[LANGUAGE_KEY] = language.name }
    }

    override suspend fun setPin(pin: CharArray?) {
        securityStore.setPin(pin)
    }

    override suspend fun verifyPin(
        pin: CharArray,
        verifier: PinVerifier,
    ): Boolean = securityStore.verify(pin, verifier)

    override suspend fun setBiometricsEnabled(enabled: Boolean) {
        securityStore.setBiometricsEnabled(enabled)
    }

    private suspend fun clearLegacySecuritySettings() {
        dataStore.edit { preferences ->
            preferences.remove(PIN_SALT_KEY)
            preferences.remove(PIN_HASH_KEY)
            preferences.remove(BIOMETRICS_ENABLED_KEY)
        }
    }

    private fun String?.toThemeMode() = ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

    private fun String?.toLanguage() = AppLanguage.entries.firstOrNull { it.name == this } ?: AppLanguage.SYSTEM

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
        val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        val BIOMETRICS_ENABLED_KEY = booleanPreferencesKey("biometrics_enabled")
    }
}
