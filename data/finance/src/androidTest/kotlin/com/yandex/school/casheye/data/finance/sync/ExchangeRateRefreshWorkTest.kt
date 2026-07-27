package com.yandex.school.casheye.data.finance.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import com.yandex.school.casheye.core.model.CurrencyCode
import com.yandex.school.casheye.domain.finance.currency.ExchangeRateRefreshResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ExchangeRateRefreshWorkTest {
    @Test
    fun temporaryFailureRequestsWorkerRetry() {
        val result =
            ExchangeRateRefreshResult
                .TemporaryFailure(cachedDataAvailable = true, cause = IOException("offline"))
                .toWorkerResult()

        assertEquals(ListenableWorker.Result.retry()::class, result::class)
    }

    @Test
    fun incompleteRatesStopWorker() {
        val result = ExchangeRateRefreshResult.Incomplete(setOf(CurrencyCode.CNY)).toWorkerResult()

        assertEquals(ListenableWorker.Result.failure()::class, result::class)
    }

    @Test
    fun freshCacheCompletesWorker() {
        val result = ExchangeRateRefreshResult.Fresh.toWorkerResult()

        assertEquals(ListenableWorker.Result.success()::class, result::class)
    }
}
