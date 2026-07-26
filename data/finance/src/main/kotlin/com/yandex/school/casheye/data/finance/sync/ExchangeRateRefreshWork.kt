package com.yandex.school.casheye.data.finance.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yandex.school.casheye.domain.finance.ExchangeRateRefreshResult
import com.yandex.school.casheye.domain.finance.ExchangeRateRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.TimeUnit

interface ExchangeRateRefreshScheduler {
    fun registerPeriodicRefresh()

    fun enqueueImmediateRefresh()
}

@Inject
@SingleIn(AppScope::class)
class WorkManagerExchangeRateRefreshScheduler(
    context: Context,
) : ExchangeRateRefreshScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun registerPeriodicRefresh() {
        val request =
            PeriodicWorkRequestBuilder<ExchangeRateRefreshWorker>(PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS)
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_DELAY_SECONDS, TimeUnit.SECONDS)
                .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    override fun enqueueImmediateRefresh() {
        val request =
            OneTimeWorkRequestBuilder<ExchangeRateRefreshWorker>()
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
        const val PERIODIC_WORK_NAME = "exchange_rate_periodic_refresh"
        const val IMMEDIATE_WORK_NAME = "exchange_rate_immediate_refresh"
        const val PERIODIC_INTERVAL_HOURS = 24L
        const val BACKOFF_DELAY_SECONDS = 30L

        val networkConstraints =
            Constraints
                .Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
    }
}

class ExchangeRateRefresher internal constructor(
    private val repository: ExchangeRateRepository,
) {
    internal suspend fun refresh(): ExchangeRateRefreshResult = repository.refreshLatest()
}

class ExchangeRateRefreshWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
    private val refresher: ExchangeRateRefresher,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result = refresher.refresh().toWorkerResult()
}

internal fun ExchangeRateRefreshResult.toWorkerResult(): ListenableWorker.Result =
    when (this) {
        ExchangeRateRefreshResult.Fresh,
        ExchangeRateRefreshResult.Updated,
        -> ListenableWorker.Result.success()

        is ExchangeRateRefreshResult.TemporaryFailure -> ListenableWorker.Result.retry()

        is ExchangeRateRefreshResult.PermanentFailure,
        is ExchangeRateRefreshResult.Incomplete,
        -> ListenableWorker.Result.failure()
    }
