package com.yandex.school.casheye.data.finance.network

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

/** App-scoped observable of validated internet availability. */
interface NetworkMonitor : AutoCloseable {
    val isOnline: StateFlow<Boolean>
}

/** Bridges [ConnectivityManager] callbacks into a lifecycle-owned online state. */
@Inject
@SingleIn(AppScope::class)
class ConnectivityManagerNetworkMonitor(
    context: Context,
) : NetworkMonitor {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val monitorScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val isOnline: StateFlow<Boolean> =
        observeConnectivity()
            .distinctUntilChanged()
            .stateIn(
                scope = monitorScope,
                started = SharingStarted.Eagerly,
                initialValue = hasValidatedInternet(),
            )

    @SuppressLint("MissingPermission")
    private fun observeConnectivity(): Flow<Boolean> =
        callbackFlow {
            val callback =
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(hasValidatedInternet())
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities,
                    ) {
                        trySend(networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
                    }

                    override fun onLost(network: Network) {
                        trySend(hasValidatedInternet())
                    }
                }

            connectivityManager.registerDefaultNetworkCallback(callback)
            trySend(hasValidatedInternet())
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        }

    @SuppressLint("MissingPermission")
    private fun hasValidatedInternet(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun close() {
        monitorScope.cancel()
    }
}
