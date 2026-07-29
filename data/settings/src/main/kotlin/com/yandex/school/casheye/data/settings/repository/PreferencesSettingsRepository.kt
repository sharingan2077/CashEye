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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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

    override suspend fun setPin(pin: CharArray?) {
        try {
            setPinVerifier(pin?.let(::createPinVerifier))
        } finally {
            pin?.fill('\u0000')
        }
    }

    override suspend fun verifyPin(
        pin: CharArray,
        verifier: PinVerifier,
    ): Boolean =
        try {
            val salt = Base64.getDecoder().decode(verifier.salt)
            val expectedHash = Base64.getDecoder().decode(verifier.hash)
            MessageDigest.isEqual(expectedHash, deriveHash(pin, salt))
        } finally {
            pin.fill('\u0000')
        }

    override suspend fun setBiometricsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[BIOMETRICS_ENABLED_KEY] =
                enabled && preferences[PIN_SALT_KEY] != null && preferences[PIN_HASH_KEY] != null
        }
    }

    private fun String?.toThemeMode(): ThemeMode = ThemeMode.entries.firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

    private fun String?.toLanguage(): AppLanguage =
        AppLanguage.entries.firstOrNull { it.name == this } ?: AppLanguage.SYSTEM

    private fun createPinVerifier(pin: CharArray): PinVerifier {
        val salt = ByteArray(PIN_SALT_BYTES).also(secureRandom::nextBytes)
        return PinVerifier(
            salt = Base64.getEncoder().encodeToString(salt),
            hash = Base64.getEncoder().encodeToString(deriveHash(pin, salt)),
        )
    }

    private fun deriveHash(
        pin: CharArray,
        salt: ByteArray,
    ): ByteArray {
        val spec = PBEKeySpec(pin, salt, PIN_HASH_ITERATIONS, PIN_HASH_BYTES * Byte.SIZE_BITS)
        return try {
            SecretKeyFactory.getInstance(PIN_HASH_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val PIN_HASH_ALGORITHM = "PBKDF2WithHmacSHA256"
        const val PIN_HASH_ITERATIONS = 210_000
        const val PIN_HASH_BYTES = 32
        const val PIN_SALT_BYTES = 16

        val secureRandom = SecureRandom()
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
        val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        val BIOMETRICS_ENABLED_KEY = booleanPreferencesKey("biometrics_enabled")
    }
}
