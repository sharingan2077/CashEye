package com.yandex.school.casheye.data.finance.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class FinanceSyncWorkTest {
    @Test
    fun temporaryFailureRequestsWorkerRetry() {
        val result = FinanceSyncResult.TemporaryFailure(IOException("offline")).toWorkerResult()

        assertEquals(ListenableWorker.Result.retry()::class, result::class)
    }

    @Test
    fun permanentFailureStopsWorker() {
        val result = FinanceSyncResult.PermanentFailure(IllegalStateException()).toWorkerResult()

        assertEquals(ListenableWorker.Result.failure()::class, result::class)
    }

    @Test
    fun successfulSyncCompletesWorker() {
        val result = FinanceSyncResult.Success.toWorkerResult()

        assertEquals(ListenableWorker.Result.success()::class, result::class)
    }
}
