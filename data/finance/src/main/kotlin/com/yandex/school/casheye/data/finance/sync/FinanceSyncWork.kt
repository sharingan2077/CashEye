package com.yandex.school.casheye.data.finance.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.TimeUnit

interface FinanceSyncScheduler {
    fun registerPeriodicSync()

    fun enqueueImmediateSync()
}

@Inject
@SingleIn(AppScope::class)
class WorkManagerFinanceSyncScheduler(
    context: Context,
) : FinanceSyncScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun registerPeriodicSync() {
        val request =
            PeriodicWorkRequestBuilder<FinanceSyncWorker>(PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
                .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun enqueueImmediateSync() {
        val request =
            OneTimeWorkRequestBuilder<FinanceSyncWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
                .build()

        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    private companion object {
        const val PERIODIC_WORK_NAME = "finance_periodic_sync"
        const val IMMEDIATE_WORK_NAME = "finance_immediate_sync"
        const val PERIODIC_INTERVAL_HOURS = 2L
        const val BACKOFF_DELAY_SECONDS = 30L

        val networkConstraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }
}

class FinanceSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val financeSyncer: FinanceSyncer,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result =
        when (financeSyncer.sync()) {
            FinanceSyncResult.Success -> Result.success()
            is FinanceSyncResult.TemporaryFailure -> Result.retry()
            is FinanceSyncResult.PermanentFailure -> Result.failure()
        }
}

class FinanceSyncWorkerFactory(
    private val financeSyncerProvider: () -> FinanceSyncer,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): CoroutineWorker? =
        when (workerClassName) {
            FinanceSyncWorker::class.java.name ->
                FinanceSyncWorker(
                    appContext = appContext,
                    workerParameters = workerParameters,
                    financeSyncer = financeSyncerProvider(),
                )

            else -> null
        }
}
