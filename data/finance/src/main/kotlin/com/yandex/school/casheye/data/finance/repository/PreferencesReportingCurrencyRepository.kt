package com.yandex.school.casheye.data.finance.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.domain.finance.currency.DEFAULT_REPORTING_CURRENCY
import com.yandex.school.casheye.domain.finance.currency.ReportingCurrencyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

internal class PreferencesReportingCurrencyRepository(
    private val dataStore: DataStore<Preferences>,
) : ReportingCurrencyRepository {
    override fun observe(): Flow<CurrencyCode> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }.map { preferences ->
                CurrencyCode.fromIsoCodeOrNull(preferences[REPORTING_CURRENCY_KEY])
                    ?: DEFAULT_REPORTING_CURRENCY
            }

    override suspend fun set(currency: CurrencyCode) {
        dataStore.edit { preferences ->
            preferences[REPORTING_CURRENCY_KEY] = currency.isoCode
        }
    }

    private companion object {
        val REPORTING_CURRENCY_KEY = stringPreferencesKey("reporting_currency")
    }
}
