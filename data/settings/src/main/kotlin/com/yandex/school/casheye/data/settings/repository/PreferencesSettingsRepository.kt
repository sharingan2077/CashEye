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
import com.yandex.school.casheye.domain.settings.SecuritySettings
import com.yandex.school.casheye.domain.settings.SettingsRepository
import com.yandex.school.casheye.domain.settings.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

internal class PreferencesSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override fun observe(): Flow<AppSettings> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                val pinVerifier =
                    preferences[PIN_SALT_KEY]?.let { salt ->
                        preferences[PIN_HASH_KEY]?.let { hash -> PinVerifier(salt = salt, hash = hash) }
                    }
                AppSettings(
                    themeMode = preferences[THEME_MODE_KEY].toThemeMode(),
                    language = preferences[LANGUAGE_KEY].toLanguage(),
                    security =
                        SecuritySettings(
                            pinVerifier = pinVerifier,
                            biometricsEnabled = pinVerifier != null && preferences[BIOMETRICS_ENABLED_KEY] == true,
                        ),
                )
            }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    override suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[LANGUAGE_KEY] = language.name }
    }

    override suspend fun setPinVerifier(verifier: PinVerifier?) {
        dataStore.edit { preferences ->
            if (verifier == null) {
                preferences.remove(PIN_SALT_KEY)
                preferences.remove(PIN_HASH_KEY)
                preferences[BIOMETRICS_ENABLED_KEY] = false
            } else {
                preferences[PIN_SALT_KEY] = verifier.salt
                preferences[PIN_HASH_KEY] = verifier.hash
            }
        }
    }

    override suspend fun setBiometricsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRICS_ENABLED_KEY] = enabled && preferences[PIN_SALT_KEY] != null && preferences[PIN_HASH_KEY] != null
        }
    }

    private fun String?.toThemeMode(): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

    private fun String?.toLanguage(): AppLanguage =
        AppLanguage.entries.firstOrNull { it.name == this } ?: AppLanguage.SYSTEM

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
        val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        val BIOMETRICS_ENABLED_KEY = booleanPreferencesKey("biometrics_enabled")
    }
}
