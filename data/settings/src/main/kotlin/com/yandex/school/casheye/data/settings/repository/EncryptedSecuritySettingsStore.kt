package com.yandex.school.casheye.data.settings.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.yandex.school.casheye.domain.settings.PinVerifier
import com.yandex.school.casheye.domain.settings.SecuritySettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal class EncryptedSecuritySettingsStore(
    context: Context,
) {
    private val preferences: SharedPreferences = createPreferences(context)

    fun observe(): Flow<SecuritySettings> =
        callbackFlow {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(settings()) }
            trySend(settings())
            preferences.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }

    fun setPin(pin: CharArray?) {
        try {
            preferences
                .edit()
                .apply {
                    if (pin == null) {
                        remove(PIN_SALT)
                        remove(PIN_HASH)
                        putBoolean(BIOMETRICS, false)
                    } else {
                        val verifier = createVerifier(pin)
                        putString(PIN_SALT, verifier.salt)
                        putString(PIN_HASH, verifier.hash)
                    }
                }.apply()
        } finally {
            pin?.fill('\u0000')
        }
    }

    fun verify(
        pin: CharArray,
        verifier: PinVerifier,
    ): Boolean =
        try {
            MessageDigest.isEqual(
                Base64.getDecoder().decode(verifier.hash),
                derive(pin, Base64.getDecoder().decode(verifier.salt)),
            )
        } finally {
            pin.fill('\u0000')
        }

    fun setBiometricsEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(BIOMETRICS, enabled && verifier() != null).apply()
    }

    private fun settings(): SecuritySettings {
        val verifier = verifier()
        return SecuritySettings(verifier, verifier != null && preferences.getBoolean(BIOMETRICS, false))
    }

    private fun verifier(): PinVerifier? =
        preferences.getString(PIN_SALT, null)?.let { salt ->
            preferences.getString(PIN_HASH, null)?.let { hash -> PinVerifier(salt, hash) }
        }

    private fun createVerifier(pin: CharArray): PinVerifier {
        val salt = ByteArray(16).also(random::nextBytes)
        return PinVerifier(
            Base64.getEncoder().encodeToString(salt),
            Base64.getEncoder().encodeToString(derive(pin, salt)),
        )
    }

    private fun derive(
        pin: CharArray,
        salt: ByteArray,
    ): ByteArray {
        val spec = PBEKeySpec(pin, salt, 210_000, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun createPreferences(context: Context): SharedPreferences =
        try {
            createEncryptedPreferences(context)
        } catch (_: GeneralSecurityException) {
            context.deleteSharedPreferences(SECURITY_PREFERENCES)
            createEncryptedPreferences(context)
        }

    private fun createEncryptedPreferences(context: Context): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            SECURITY_PREFERENCES,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private companion object {
        const val SECURITY_PREFERENCES = "encrypted_security_settings"
        const val PIN_SALT = "pin_salt"
        const val PIN_HASH = "pin_hash"
        const val BIOMETRICS = "biometrics_enabled"
        val random = SecureRandom()
    }
}
