package com.yandex.school.casheye.app

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.work.Configuration
import com.yandex.school.casheye.BuildConfig
import com.yandex.school.casheye.app.di.AppGraph
import com.yandex.school.casheye.data.finance.di.NetworkConfig
import com.yandex.school.casheye.data.finance.sync.FinanceSyncWorkerFactory
import dev.zacsweers.metro.createGraphFactory

class CashEyeApplication : Application(), Configuration.Provider {
    lateinit var appGraph: AppGraph
        private set

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(FinanceSyncWorkerFactory { appGraph.financeSyncer })
            .build()
    }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                appGraph.financeSyncScheduler.enqueueImmediateSync()
            }
        }

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
        getSystemService(ConnectivityManager::class.java).registerDefaultNetworkCallback(networkCallback)
    }
}
