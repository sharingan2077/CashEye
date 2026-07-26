package com.yandex.school.casheye.data.finance.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.domain.finance.DEFAULT_REPORTING_CURRENCY
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class PreferencesReportingCurrencyRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `uses the shared default when no preference is stored`() =
        runTest {
            val repository = repository(this)

            assertEquals(DEFAULT_REPORTING_CURRENCY, repository.observe().first())
        }

    @Test
    fun `stores and observes the selected currency`() =
        runTest {
            val repository = repository(this)
            val observed = async { repository.observe().take(2).toList() }
            runCurrent()

            repository.set(CurrencyCode.USD)

            assertEquals(listOf(DEFAULT_REPORTING_CURRENCY, CurrencyCode.USD), observed.await())
        }

    @Test
    fun `falls back to the shared default for an unknown stored code`() =
        runTest {
            val dataStore = dataStore(this)
            dataStore.edit { it[stringPreferencesKey("reporting_currency")] = "JPY" }
            val repository = PreferencesReportingCurrencyRepository(dataStore)

            assertEquals(DEFAULT_REPORTING_CURRENCY, repository.observe().first())
        }

    private fun repository(scope: TestScope): PreferencesReportingCurrencyRepository {
        return PreferencesReportingCurrencyRepository(dataStore(scope))
    }

    private fun dataStore(scope: TestScope) =
        PreferenceDataStoreFactory.create(
            scope = scope.backgroundScope,
            produceFile = { File(temporaryFolder.root, "reporting_currency.preferences_pb") },
        )
}
