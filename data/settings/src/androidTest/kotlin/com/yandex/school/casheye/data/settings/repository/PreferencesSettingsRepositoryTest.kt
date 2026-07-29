package com.yandex.school.casheye.data.settings.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.yandex.school.casheye.domain.settings.SecuritySettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesSettingsRepositoryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val securityStore = EncryptedSecuritySettingsStore(context)

    @After
    fun clearSecuritySettings() {
        securityStore.setPin(null)
    }

    @Test
    fun `stores verifier, verifies pin, and clears biometric state with pin`() =
        runTest {
            securityStore.setPin("1234".toCharArray())
            val verifier = securityStore.observe().first().pinVerifier

            assertNotNull(verifier)
            assertTrue(securityStore.verify("1234".toCharArray(), requireNotNull(verifier)))
            assertFalse(securityStore.verify("9999".toCharArray(), requireNotNull(verifier)))

            securityStore.setBiometricsEnabled(true)
            assertTrue(securityStore.observe().first().biometricsEnabled)

            securityStore.setPin(null)
            assertEqualsSecurity(SecuritySettings(), securityStore.observe().first())
        }

    @Test
    fun `first observation removes legacy unencrypted security keys`() =
        runTest {
            val dataStore = dataStore(this)
            dataStore.edit { preferences ->
                preferences[stringPreferencesKey("pin_salt")] = "legacy-salt"
                preferences[stringPreferencesKey("pin_hash")] = "legacy-hash"
                preferences[booleanPreferencesKey("biometrics_enabled")] = true
            }
            val repository = PreferencesSettingsRepository(dataStore, securityStore)

            repository.observe().first()
            val preferences = dataStore.data.first()

            assertNull(preferences[stringPreferencesKey("pin_salt")])
            assertNull(preferences[stringPreferencesKey("pin_hash")])
            assertNull(preferences[booleanPreferencesKey("biometrics_enabled")])
        }

    private fun dataStore(scope: TestScope) =
        PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = {
                File(context.filesDir, "settings-test-${UUID.randomUUID()}.preferences_pb")
            },
        )

    private fun assertEqualsSecurity(
        expected: SecuritySettings,
        actual: SecuritySettings,
    ) {
        assertNull(expected.pinVerifier)
        assertNull(actual.pinVerifier)
        assertFalse(actual.biometricsEnabled)
    }
}
