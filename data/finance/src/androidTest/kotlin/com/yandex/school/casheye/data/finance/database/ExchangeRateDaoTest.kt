package com.yandex.school.casheye.data.finance.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateCoverageEntity
import com.yandex.school.casheye.data.finance.database.entity.ExchangeRateEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExchangeRateDaoTest {
    private lateinit var database: FinanceDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database =
            Room
                .inMemoryDatabaseBuilder(context, FinanceDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun upsertIsIdempotentAndLatestReturnsNewestRatePerCurrency() =
        runTest {
            val dao = database.exchangeRateDao()
            dao.upsertRates(
                listOf(
                    rate("USD", "1.10", "2026-07-23"),
                    rate("USD", "1.20", "2026-07-24"),
                    rate("GBP", "0.80", "2026-07-24"),
                ),
            )
            dao.upsertRates(listOf(rate("USD", "1.25", "2026-07-24")))

            val latest = dao.observeLatest().first()

            assertEquals(2, latest.size)
            assertEquals("1.25", latest.single { it.quoteCurrency == "USD" }.rate)
        }

    @Test
    fun rangeAndCoverageQueriesUseRequestedBounds() =
        runTest {
            val dao = database.exchangeRateDao()
            dao.upsertRates(
                listOf(
                    rate("USD", "1.10", "2026-07-22"),
                    rate("USD", "1.20", "2026-07-24"),
                    rate("USD", "1.30", "2026-07-26"),
                ),
            )
            dao.upsertCoverage(ExchangeRateCoverageEntity("2026-07-20", "2026-07-25", 100))

            assertEquals(1, dao.getRange("2026-07-23", "2026-07-25").size)
            assertEquals(1, dao.getCoverages("2026-07-24", "2026-07-26").size)
        }

    private fun rate(
        quote: String,
        value: String,
        date: String,
    ) = ExchangeRateEntity(
        baseCurrency = "EUR",
        quoteCurrency = quote,
        rate = value,
        rateDate = date,
        fetchedAt = 100,
    )
}
