package com.yandex.school.casheye.data.finance.repository

import com.yandex.school.casheye.data.finance.api.ExchangeRateApi
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateCoverageEntity
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateEntity
import com.yandex.school.casheye.data.finance.dto.ExchangeRateDto
import com.yandex.school.casheye.domain.finance.ExchangeRateRefreshResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class ExchangeRateRepositoryImplTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-26T10:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `range refresh uses one range request and does not reload covered period`() =
        runTest {
            val api = FakeExchangeRateApi()
            val cache = FakeExchangeRateCache()
            val repository = RoomExchangeRateRepository(api, cache, clock)
            val start = LocalDate.of(2026, 7, 20)
            val end = LocalDate.of(2026, 7, 26)

            assertEquals(ExchangeRateRefreshResult.Updated, repository.refreshRange(start, end))
            assertEquals(ExchangeRateRefreshResult.Fresh, repository.refreshRange(start, end))

            assertEquals(1, api.requests.size)
            assertEquals(start.minusDays(7).toString(), api.requests.single().first)
            assertEquals(end.toString(), api.requests.single().second)
        }

    @Test
    fun `fresh complete latest cache skips network`() =
        runTest {
            val api = FakeExchangeRateApi()
            val cache = FakeExchangeRateCache()
            cache.upsertRates(api.fixture())
            val repository = RoomExchangeRateRepository(api, cache, clock)

            val result = repository.refreshLatest()

            assertEquals(ExchangeRateRefreshResult.Fresh, result)
            assertTrue(api.requests.isEmpty())
        }

    private class FakeExchangeRateApi : ExchangeRateApi {
        val requests = mutableListOf<Pair<String?, String?>>()

        override suspend fun getRates(
            base: String,
            quotes: String,
            from: String?,
            to: String?,
        ): List<ExchangeRateDto> {
            requests += from to to
            return fixtureDtos()
        }

        fun fixture(): List<ExchangeRateEntity> =
            fixtureDtos().map {
                ExchangeRateEntity(
                    baseCurrency = it.base,
                    quoteCurrency = it.quote,
                    rate = it.rate.toPlainString(),
                    rateDate = it.date.toString(),
                    fetchedAt = Instant.parse("2026-07-26T09:00:00Z").toEpochMilli(),
                )
            }

        private fun fixtureDtos(): List<ExchangeRateDto> =
            listOf("RUB" to "90", "USD" to "1.2", "GBP" to "0.8", "CNY" to "8").map { (quote, rate) ->
                ExchangeRateDto(
                    date = LocalDate.of(2026, 7, 24),
                    base = "EUR",
                    quote = quote,
                    rate = BigDecimal(rate),
                )
            }
    }

    private class FakeExchangeRateCache : ExchangeRateCache {
        private val rates = MutableStateFlow<List<ExchangeRateEntity>>(emptyList())
        private val coverages = mutableListOf<ExchangeRateCoverageEntity>()

        override fun observeLatest(): Flow<List<ExchangeRateEntity>> = rates

        override fun observeRange(
            startDate: String,
            endDate: String,
        ): Flow<List<ExchangeRateEntity>> = rates

        override suspend fun getLatest(): List<ExchangeRateEntity> = rates.value

        override suspend fun getRange(
            startDate: String,
            endDate: String,
        ): List<ExchangeRateEntity> = rates.value.filter { it.rateDate in startDate..endDate }

        override suspend fun latestFetchedAt(): Long? = rates.value.maxOfOrNull(ExchangeRateEntity::fetchedAt)

        override suspend fun getCoverages(
            startDate: String,
            endDate: String,
        ): List<ExchangeRateCoverageEntity> = coverages.filter { it.endDate >= startDate && it.startDate <= endDate }

        override suspend fun upsertRates(rates: List<ExchangeRateEntity>) {
            val keyed =
                this.rates.value
                    .associateBy { Triple(it.baseCurrency, it.quoteCurrency, it.rateDate) }
                    .toMutableMap()
            rates.forEach { keyed[Triple(it.baseCurrency, it.quoteCurrency, it.rateDate)] = it }
            this.rates.value = keyed.values.toList()
        }

        override suspend fun upsertCoverage(coverage: ExchangeRateCoverageEntity) {
            coverages.removeAll { it.startDate == coverage.startDate && it.endDate == coverage.endDate }
            coverages += coverage
        }
    }
}
