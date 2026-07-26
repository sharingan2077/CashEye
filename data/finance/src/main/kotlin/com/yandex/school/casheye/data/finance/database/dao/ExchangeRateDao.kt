package com.yandex.school.casheye.data.finance.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateCoverageEntity
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ExchangeRateDao {
    @Query(
        """
        SELECT rates.* FROM exchange_rates AS rates
        INNER JOIN (
            SELECT base_currency, quote_currency, MAX(rate_date) AS latest_date
            FROM exchange_rates
            GROUP BY base_currency, quote_currency
        ) AS latest
        ON rates.base_currency = latest.base_currency
           AND rates.quote_currency = latest.quote_currency
           AND rates.rate_date = latest.latest_date
        ORDER BY rates.quote_currency
        """,
    )
    fun observeLatest(): Flow<List<ExchangeRateEntity>>

    @Query(
        """
        SELECT * FROM exchange_rates
        WHERE rate_date BETWEEN :startDate AND :endDate
        ORDER BY rate_date, quote_currency
        """,
    )
    fun observeRange(
        startDate: String,
        endDate: String,
    ): Flow<List<ExchangeRateEntity>>

    @Query(
        """
        SELECT rates.* FROM exchange_rates AS rates
        INNER JOIN (
            SELECT base_currency, quote_currency, MAX(rate_date) AS latest_date
            FROM exchange_rates
            GROUP BY base_currency, quote_currency
        ) AS latest
        ON rates.base_currency = latest.base_currency
           AND rates.quote_currency = latest.quote_currency
           AND rates.rate_date = latest.latest_date
        ORDER BY rates.quote_currency
        """,
    )
    suspend fun getLatest(): List<ExchangeRateEntity>

    @Query(
        """
        SELECT * FROM exchange_rates
        WHERE rate_date BETWEEN :startDate AND :endDate
        ORDER BY rate_date, quote_currency
        """,
    )
    suspend fun getRange(
        startDate: String,
        endDate: String,
    ): List<ExchangeRateEntity>

    @Query("SELECT MAX(fetched_at) FROM exchange_rates")
    suspend fun latestFetchedAt(): Long?

    @Query(
        """
        SELECT * FROM exchange_rate_coverages
        WHERE end_date >= :startDate AND start_date <= :endDate
        ORDER BY start_date
        """,
    )
    suspend fun getCoverages(
        startDate: String,
        endDate: String,
    ): List<ExchangeRateCoverageEntity>

    @Upsert
    suspend fun upsertRates(rates: List<ExchangeRateEntity>)

    @Upsert
    suspend fun upsertCoverage(coverage: ExchangeRateCoverageEntity)
}
