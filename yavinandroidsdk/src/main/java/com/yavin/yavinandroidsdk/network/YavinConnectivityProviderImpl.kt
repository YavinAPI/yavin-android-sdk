package com.yavin.yavinandroidsdk.network

import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.TRANSPORT_CELLULAR
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.yavin.yavinandroidsdk.network.YavinConnectivityProvider.NetworkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket


class YavinConnectivityProviderImpl(
    private val connectivityManager: ConnectivityManager,
    private val wifiManager: WifiManager
) : YavinConnectivityProvider {

    private val logName = this::class.java.simpleName
    private val handler = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<YavinConnectivityProvider.ConnectivityStateListener>()
    private var subscribed = false
    private var connectivityPollingJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private lateinit var currentNetworkState: NetworkState

    init {
        initNetworkStateUsingCapabilitiesOnly()
    }

    private val networkRequestBuilder: NetworkRequest.Builder = NetworkRequest.Builder()
        .addTransportType(TRANSPORT_WIFI)
        .addTransportType(TRANSPORT_ETHERNET)
        .addTransportType(TRANSPORT_CELLULAR)

    private val networkCallback = object : NetworkCallback() {

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            Log.e(logName, "onCapChange: $network, $networkCapabilities")

            if (networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET)) {

                if (networkCapabilities.hasTransport(TRANSPORT_WIFI)) {
                    Log.d(
                        logName,
                        "Connected to a wifi network. Do not change connectivity status and start connectivity polling"
                    )
                    startPollingConnectivity()
                } else {
                    Log.d(
                        logName,
                        "Connected to a non-wifi network with NET_CAPABILITY_INTERNET. " +
                            "Consider that internet connection is active. Do not start connectivity polling"
                    )

                    val newNetworkState = NetworkState(
                        true,
                        connectivityManager.getNetworkCapabilities(network)
                            ?.let { getTransportFromNetworkCapabilities(it) }
                    )
                    setNetworkState(newNetworkState)
                }

            } else {
                //otherwise stop doing that and dispatch no connectivity state
                stopConnectivityPolling()
                val newNetworkState = NetworkState(
                    false,
                    connectivityManager.getNetworkCapabilities(network)
                        ?.let { getTransportFromNetworkCapabilities(it) }
                )
                setNetworkState(newNetworkState)
            }
        }

        override fun onLost(network: Network) {
            stopConnectivityPolling()
            setNetworkState(NetworkState(false))
        }
    }

    private fun setNetworkState(newState: NetworkState) {
        if (newState != currentNetworkState) {
            Log.v(
                logName,
                "Connectivity state changed. Dispatching new state with hasInternet = ${newState.hasInternet}"
            )
            currentNetworkState = newState
            handler.post {
                for (listener in listeners) {
                    listener.onConnectivityStateChange(currentNetworkState)
                }
            }
        }
    }

    override fun getActiveNetwork() = connectivityManager.activeNetwork

    override fun addListener(listener: YavinConnectivityProvider.ConnectivityStateListener) {
        listeners.add(listener)
        listener.onConnectivityStateChange(getNetworkState())
        verifySubscription()
    }

    override fun removeListener(listener: YavinConnectivityProvider.ConnectivityStateListener) {
        listeners.remove(listener)
        verifySubscription()
    }

    fun getNetworkState(): NetworkState = currentNetworkState

    private fun initNetworkStateUsingCapabilitiesOnly() {
        Log.v(logName, "initNetworkStateUsingCapabilities()")
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        currentNetworkState = if (capabilities != null) {
            NetworkState(
                hasInternet = capabilities.hasCapability(NET_CAPABILITY_INTERNET),
                networkTransportType = getTransportFromNetworkCapabilities(capabilities)
            )
        } else {
            NetworkState(false)
        }
    }

    private fun stopConnectivityPolling() {
        Log.v(logName, "stopPollingConnectivity()")
        connectivityPollingJob?.cancel()
        connectivityPollingJob = null
    }

    private fun startPollingConnectivity() {
        if (connectivityPollingJob?.isActive == true) {
            Log.v(
                logName,
                "connectivity polling job IS ACTIVE - ignore startPollingConnectivityService() invocation"
            )
            return
        } else {
            Log.v(
                logName,
                "connectivity polling job IS NOT ACTIVE - starting startPollingConnectivityService()"
            )
        }

        connectivityPollingJob = coroutineScope.launch {
            while (isActive) {
                val hasConnectionToGoogle = connectToGoogleServer()
                Log.v(logName, "hasConnectionToGoogle = $hasConnectionToGoogle")
                withContext(Dispatchers.Main) {
                    ensureActive()
                    val capabilities =
                        connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                    val newNetworkState = NetworkState(
                        hasInternet = hasConnectionToGoogle,
                        networkTransportType = if (capabilities != null) {
                            getTransportFromNetworkCapabilities(capabilities)
                        } else {
                            null
                        }
                    )
                    setNetworkState(newNetworkState)
                    delay(CONNECTIVITY_REQUEST_INTERVAL)
                }
            }
        }
    }

    private fun connectToGoogleServer(): Boolean {
        Log.v(logName, "connectToGoogleServer")
        return try {
            val socket = Socket()
            socket.connect(
                InetSocketAddress(GOOGLE_SERVICE_IP, GOOGLE_SERVICE_PORT),
                GOOGLE_SERVICE_TIMEOUT
            )
            socket.close()
            true
        } catch (e: Exception) {
            Log.v(logName, "connect to google server failed")
            false
        }
    }

    private fun getTransportFromNetworkCapabilities(networkCapabilities: NetworkCapabilities): Int? {
        return when {
            networkCapabilities.hasTransport(TRANSPORT_WIFI) -> TRANSPORT_WIFI
            networkCapabilities.hasTransport(TRANSPORT_CELLULAR) -> TRANSPORT_CELLULAR
            networkCapabilities.hasTransport(TRANSPORT_ETHERNET) -> TRANSPORT_ETHERNET
            else -> null
        }
    }

    private fun verifySubscription() {
        if (!subscribed && listeners.isNotEmpty()) {
            registerNetworkCallback()
            subscribed = true
        } else if (subscribed && listeners.isEmpty()) {
            unregisterNetworkCallback()
            stopConnectivityPolling()
            subscribed = false
        }
    }

    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            connectivityManager.registerNetworkCallback(
                networkRequestBuilder.build(),
                networkCallback
            )
        }
    }

    private fun unregisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun hasInternet(): Boolean {
        return getNetworkState().hasInternet
    }

    override fun isWifiEnabled() = wifiManager.isWifiEnabled

    override fun isWifiConnected(): Boolean {
        val currentNetwork = getActiveNetwork()
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps?.hasTransport(TRANSPORT_WIFI) == true && caps.hasCapability(
            NET_CAPABILITY_INTERNET
        )
    }

    override fun isMobileDataConnected(): Boolean {
        val currentNetwork = getActiveNetwork()
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps?.hasTransport(TRANSPORT_CELLULAR) == true && caps.hasCapability(
            NET_CAPABILITY_INTERNET
        )
    }

    override fun isEthernetConnected(): Boolean {
        val currentNetwork = getActiveNetwork()
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps?.hasTransport(TRANSPORT_ETHERNET) == true && caps.hasCapability(
            NET_CAPABILITY_INTERNET
        )
    }

    override fun getLocalIpAddress(): String? {
        try {
            val linkAddresses =
                (connectivityManager.getLinkProperties(connectivityManager.activeNetwork) as LinkProperties).linkAddresses
            val ipAddressV4 = linkAddresses.firstOrNull {
                it.address.hostAddress?.contains(".") ?: false
            }?.address?.hostAddress
            return ipAddressV4
        } catch (exception: Exception) {
            Log.e(logName, exception.stackTraceToString())
        }

        return null
    }

    companion object {
        const val CONNECTIVITY_REQUEST_INTERVAL = 20000L
        const val GOOGLE_SERVICE_TIMEOUT = 10000
        const val GOOGLE_SERVICE_IP = "8.8.8.8"
        const val GOOGLE_SERVICE_PORT = 53
    }
}