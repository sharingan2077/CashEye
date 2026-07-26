package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.data.finance.api.ExchangeRateApi
import com.yandex.school.casheye.data.finance.database.FinanceDatabaseProvider
import com.yandex.school.casheye.data.finance.database.dao.ExchangeRateDao
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateCoverageEntity
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateEntity
import com.yandex.school.casheye.data.finance.dto.ExchangeRateDto
import com.yandex.school.casheye.domain.finance.ExchangeRate
import com.yandex.school.casheye.domain.finance.ExchangeRateRefreshResult
import com.yandex.school.casheye.domain.finance.ExchangeRateRepository
import com.yandex.school.casheye.domain.finance.ExchangeRateSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.LocalDate

internal class RoomExchangeRateRepository(
    private val api: ExchangeRateApi,
    private val cache: ExchangeRateCache,
    private val clock: Clock = Clock.systemUTC(),
) : ExchangeRateRepository {
    constructor(
        api: ExchangeRateApi,
        databaseProvider: FinanceDatabaseProvider,
        clock: Clock = Clock.systemUTC(),
    ) : this(api, RoomExchangeRateCache(databaseProvider.database.exchangeRateDao()), clock)

    override fun observeLatest(): Flow<ExchangeRateSnapshot> =
        cache.observeLatest().map { entities ->
            entities.toSnapshot(requestedFrom = null, requestedTo = null)
        }

    override fun observeRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Flow<ExchangeRateSnapshot> {
        require(!endDate.isBefore(startDate)) { "End date must not be before start date" }
        val cacheStart = startDate.minusDays(HISTORICAL_LOOKBACK_DAYS)
        return cache.observeRange(cacheStart.toString(), endDate.toString()).map { entities ->
            entities.toSnapshot(requestedFrom = startDate, requestedTo = endDate)
        }
    }

    override suspend fun refreshLatest(force: Boolean): ExchangeRateRefreshResult {
        val cached = cache.getLatest()
        val fetchedAt = cache.latestFetchedAt()
        if (!force && cached.hasAllQuotes() && fetchedAt != null && isFresh(fetchedAt)) {
            return ExchangeRateRefreshResult.Fresh
        }

        return runCatching { api.getRates() }
            .fold(
                onSuccess = { rates ->
                    storeLatest(rates)
                },
                onFailure = { error ->
                    error.toRefreshFailure(cached.hasAllQuotes())
                },
            )
    }

    override suspend fun refreshRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): ExchangeRateRefreshResult {
        require(!endDate.isBefore(startDate)) { "End date must not be before start date" }
        val requestedStart = startDate.minusDays(HISTORICAL_LOOKBACK_DAYS)
        val missingRanges = missingRanges(requestedStart, endDate)
        if (missingRanges.isEmpty() && cachedRangeIsComplete(requestedStart, endDate)) {
            return ExchangeRateRefreshResult.Fresh
        }

        for (range in missingRanges) {
            val response =
                try {
                    api.getRates(from = range.start.toString(), to = range.endInclusive.toString())
                } catch (error: Throwable) {
                    return error.toRefreshFailure(cachedRangeIsComplete(requestedStart, endDate))
                }
            val missing = missingQuotes(response)
            if (missing.isNotEmpty()) {
                storeRates(response)
                return ExchangeRateRefreshResult.Incomplete(missing)
            }
            storeRates(response)
            cache.upsertCoverage(
                ExchangeRateCoverageEntity(
                    startDate = range.start.toString(),
                    endDate = range.endInclusive.toString(),
                    fetchedAt = clock.millis(),
                ),
            )
        }
        return ExchangeRateRefreshResult.Updated
    }

    private suspend fun storeLatest(rates: List<ExchangeRateDto>): ExchangeRateRefreshResult {
        val missing = missingQuotes(rates)
        storeRates(rates)
        return if (missing.isEmpty()) {
            ExchangeRateRefreshResult.Updated
        } else {
            ExchangeRateRefreshResult.Incomplete(missing)
        }
    }

    private suspend fun storeRates(rates: List<ExchangeRateDto>) {
        val validRates =
            rates.filter {
                it.base.equals(CurrencyCode.EUR.isoCode, ignoreCase = true) &&
                    CurrencyCode.fromIsoCodeOrNull(it.quote) in QUOTE_CURRENCIES &&
                    it.rate > BigDecimal.ZERO
            }
        val fetchedAt = clock.millis()
        val entities =
            validRates.map { it.toEntity(fetchedAt) } +
                validRates
                    .map(ExchangeRateDto::date)
                    .distinct()
                    .map { date ->
                        ExchangeRateEntity(
                            baseCurrency = CurrencyCode.EUR.isoCode,
                            quoteCurrency = CurrencyCode.EUR.isoCode,
                            rate = BigDecimal.ONE.toPlainString(),
                            rateDate = date.toString(),
                            fetchedAt = fetchedAt,
                        )
                    }
        if (entities.isNotEmpty()) cache.upsertRates(entities)
    }

    private suspend fun missingRanges(
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ClosedRange<LocalDate>> {
        val coverages =
            cache
                .getCoverages(startDate.toString(), endDate.toString())
                .map { LocalDate.parse(it.startDate)..LocalDate.parse(it.endDate) }
                .sortedBy { it.start }
        val missing = mutableListOf<ClosedRange<LocalDate>>()
        var cursor = startDate
        coverages.forEach { coverage ->
            if (coverage.endInclusive.isBefore(cursor)) return@forEach
            if (coverage.start.isAfter(cursor)) {
                missing += cursor..minOf(endDate, coverage.start.minusDays(1))
            }
            if (!coverage.endInclusive.isBefore(cursor)) {
                cursor = coverage.endInclusive.plusDays(1)
            }
        }
        if (!cursor.isAfter(endDate)) missing += cursor..endDate
        return missing
    }

    private suspend fun cachedRangeIsComplete(
        startDate: LocalDate,
        endDate: LocalDate,
    ): Boolean =
        missingRanges(startDate, endDate).isEmpty() &&
            cache.getRange(startDate.toString(), endDate.toString()).hasAllQuotes()

    private fun isFresh(fetchedAt: Long): Boolean = Duration.ofMillis(clock.millis() - fetchedAt) < LATEST_CACHE_TTL

    private fun missingQuotes(rates: List<ExchangeRateDto>): Set<CurrencyCode> {
        val available =
            rates
                .asSequence()
                .filter { it.base.equals(CurrencyCode.EUR.isoCode, ignoreCase = true) }
                .mapNotNull { CurrencyCode.fromIsoCodeOrNull(it.quote) }
                .toSet()
        return QUOTE_CURRENCIES - available
    }

    private fun List<ExchangeRateEntity>.toSnapshot(
        requestedFrom: LocalDate?,
        requestedTo: LocalDate?,
    ): ExchangeRateSnapshot =
        ExchangeRateSnapshot(
            rates = map { entity -> entity.toDomain() },
            requestedFrom = requestedFrom,
            requestedTo = requestedTo,
            missingCurrencies =
                QUOTE_CURRENCIES -
                    mapNotNull { CurrencyCode.fromIsoCodeOrNull(it.quoteCurrency) }.toSet(),
        )

    private fun List<ExchangeRateEntity>.hasAllQuotes(): Boolean =
        mapNotNull { CurrencyCode.fromIsoCodeOrNull(it.quoteCurrency) }.toSet().containsAll(QUOTE_CURRENCIES)

    private fun Throwable.toRefreshFailure(cachedDataAvailable: Boolean): ExchangeRateRefreshResult =
        when {
            this is CancellationException -> {
                throw this
            }

            this is IOException -> {
                ExchangeRateRefreshResult.TemporaryFailure(cachedDataAvailable, this)
            }

            this is HttpException && code() in 500..599 -> {
                ExchangeRateRefreshResult.TemporaryFailure(cachedDataAvailable, this)
            }

            else -> {
                ExchangeRateRefreshResult.PermanentFailure(cachedDataAvailable, this)
            }
        }

    private fun ExchangeRateDto.toEntity(fetchedAt: Long): ExchangeRateEntity =
        ExchangeRateEntity(
            baseCurrency = CurrencyCode.EUR.isoCode,
            quoteCurrency = CurrencyCode.fromIsoCode(quote).isoCode,
            rate = rate.toPlainString(),
            rateDate = date.toString(),
            fetchedAt = fetchedAt,
        )

    private fun ExchangeRateEntity.toDomain(): ExchangeRate =
        ExchangeRate(
            baseCurrency = CurrencyCode.fromIsoCode(baseCurrency),
            quoteCurrency = CurrencyCode.fromIsoCode(quoteCurrency),
            rate = BigDecimal(rate),
            date = LocalDate.parse(rateDate),
        )

    private companion object {
        val QUOTE_CURRENCIES = CurrencyCode.entries.toSet() - CurrencyCode.EUR
        val LATEST_CACHE_TTL: Duration = Duration.ofHours(24)
        const val HISTORICAL_LOOKBACK_DAYS = 7L
    }
}

internal interface ExchangeRateCache {
    fun observeLatest(): Flow<List<ExchangeRateEntity>>

    fun observeRange(
        startDate: String,
        endDate: String,
    ): Flow<List<ExchangeRateEntity>>

    suspend fun getLatest(): List<ExchangeRateEntity>

    suspend fun getRange(
        startDate: String,
        endDate: String,
    ): List<ExchangeRateEntity>

    suspend fun latestFetchedAt(): Long?

    suspend fun getCoverages(
        startDate: String,
        endDate: String,
    ): List<ExchangeRateCoverageEntity>

    suspend fun upsertRates(rates: List<ExchangeRateEntity>)

    suspend fun upsertCoverage(coverage: ExchangeRateCoverageEntity)
}

private class RoomExchangeRateCache(
    private val dao: ExchangeRateDao,
) : ExchangeRateCache {
    override fun observeLatest(): Flow<List<ExchangeRateEntity>> = dao.observeLatest()

    override fun observeRange(
        startDate: String,
        endDate: String,
    ): Flow<List<ExchangeRateEntity>> = dao.observeRange(startDate, endDate)

    override suspend fun getLatest(): List<ExchangeRateEntity> = dao.getLatest()

    override suspend fun getRange(
        startDate: String,
        endDate: String,
    ): List<ExchangeRateEntity> = dao.getRange(startDate, endDate)

    override suspend fun latestFetchedAt(): Long? = dao.latestFetchedAt()

    override suspend fun getCoverages(
        startDate: String,
        endDate: String,
    ): List<ExchangeRateCoverageEntity> = dao.getCoverages(startDate, endDate)

    override suspend fun upsertRates(rates: List<ExchangeRateEntity>) {
        dao.upsertRates(rates)
    }

    override suspend fun upsertCoverage(coverage: ExchangeRateCoverageEntity) {
        dao.upsertCoverage(coverage)
    }
}
