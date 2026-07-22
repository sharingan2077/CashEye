package com.yandex.school.casheye.app

import android.app.Application
import com.yandex.school.casheye.BuildConfig
import com.yandex.school.casheye.app.di.AppGraph
import com.yandex.school.casheye.data.finance.di.NetworkConfig
import dev.zacsweers.metro.createGraphFactory

class CashEyeApplication : Application() {
    lateinit var appGraph: AppGraph
        private set

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
    }
}
