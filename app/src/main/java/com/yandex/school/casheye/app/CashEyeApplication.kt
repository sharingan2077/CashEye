package com.yandex.school.casheye.app

import android.app.Application
import androidx.work.Configuration
import com.yandex.school.casheye.BuildConfig
import com.yandex.school.casheye.app.di.AppGraph
import com.yandex.school.casheye.data.finance.di.NetworkConfig
import com.yandex.school.casheye.data.finance.sync.FinanceSyncWorkerFactory
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CashEyeApplication : Application(), Configuration.Provider {
    lateinit var appGraph: AppGraph
        private set

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(FinanceSyncWorkerFactory { appGraph.financeSyncer })
            .build()
    }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appGraph =
            createGraphFactory<AppGraph.Factory>().create(
                networkConfig =
                    NetworkConfig(
                        apiKey = BuildConfig.API_KEY,
                        isLoggingEnabled = BuildConfig.DEBUG,
                    ),
                context = this,
            )
        appGraph.financeSyncScheduler.registerPeriodicSync()
        appGraph.financeSyncScheduler.enqueueImmediateSync()
        observeNetworkRecovery()
    }

    private fun observeNetworkRecovery() {
        val initialOnline = appGraph.networkMonitor.isOnline.value
        applicationScope.launch {
            var wasOnline = initialOnline
            appGraph.networkMonitor.isOnline.collect { isOnline ->
                if (wasOnline == false && isOnline) {
                    appGraph.financeSyncScheduler.enqueueImmediateSync()
                }
                wasOnline = isOnline
            }
        }
    }

    override fun onTerminate() {
        applicationScope.cancel()
        if (::appGraph.isInitialized) {
            appGraph.networkMonitor.close()
        }
        super.onTerminate()
    }
}
