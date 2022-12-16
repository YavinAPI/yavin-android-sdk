package com.yavin.yavinandroidsdk.network

import android.net.*
import android.net.ConnectivityManager.NetworkCallback
import android.net.NetworkCapabilities.*
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.yavin.yavinandroidsdk.network.YavinConnectivityProvider.NetworkState


class YavinConnectivityProviderImpl(
    private val connectivityManager: ConnectivityManager,
    private val wifiManager: WifiManager
) : YavinConnectivityProvider {

    private val logName = this::class.java.simpleName

    private val handler = Handler(Looper.getMainLooper())
    private val listeners = mutableSetOf<YavinConnectivityProvider.ConnectivityStateListener>()
    private var subscribed = false

    private lateinit var currentNetworkState: NetworkState

    init {
        updateNetworkState()
    }

    private val networkRequestBuilder: NetworkRequest.Builder = NetworkRequest.Builder()
        .addTransportType(TRANSPORT_WIFI)
        .addTransportType(TRANSPORT_ETHERNET)
        .addTransportType(TRANSPORT_CELLULAR)

    private val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.v(logName, "onAvailable: $network, ${connectivityManager.getNetworkCapabilities(network)?.let { getTransportFromNetworkCapabilities(it) }}")
            val newNetworkState = NetworkState(
                false,
                connectivityManager.getNetworkCapabilities(network)?.let { getTransportFromNetworkCapabilities(it) }
            )
            if (getActiveNetwork() == null && getNetworkState() != newNetworkState) {
                currentNetworkState = newNetworkState
                dispatchChange(newNetworkState)
            } else {
                Log.v(logName, "Already have an active network: ${getActiveNetwork()}")
            }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            Log.e(logName, "onCapChange: $network, $networkCapabilities")
            val newNetworkState = NetworkState(
                networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET),
                getTransportFromNetworkCapabilities(networkCapabilities)
            )

            if (getNetworkState() != newNetworkState) {
                currentNetworkState = newNetworkState
                dispatchChange(newNetworkState)
            }
        }

        override fun onLost(network: Network) {
            val newNetworkState = NetworkState(false)

            if (getNetworkState() != newNetworkState) {
                currentNetworkState = newNetworkState
                dispatchChange(newNetworkState)
            }
        }
    }

    private fun dispatchChange(state: NetworkState) {
        handler.post {
            for (listener in listeners) {
                listener.onConnectivityStateChange(state)
            }
        }
    }

    override fun getActiveNetwork() = connectivityManager.activeNetwork

    override fun addListener(listener: YavinConnectivityProvider.ConnectivityStateListener) {
        listeners.add(listener)
        updateNetworkState()
        listener.onConnectivityStateChange(getNetworkState())
        verifySubscription()
    }

    override fun removeListener(listener: YavinConnectivityProvider.ConnectivityStateListener) {
        listeners.remove(listener)
        verifySubscription()
    }

    private fun verifySubscription() {
        if (!subscribed && listeners.isNotEmpty()) {
            registerNetworkCallback()
            subscribed = true
        } else if (subscribed && listeners.isEmpty()) {
            unregisterNetworkCallback()
            subscribed = false
        }
    }

    private fun registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            connectivityManager.registerNetworkCallback(networkRequestBuilder.build(), networkCallback)
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
        return caps?.hasTransport(TRANSPORT_WIFI) == true && caps.hasCapability(NET_CAPABILITY_INTERNET)
    }

    override fun isEthernetConnected(): Boolean {
        val currentNetwork = getActiveNetwork()
        val caps = connectivityManager.getNetworkCapabilities(currentNetwork)
        return caps?.hasTransport(TRANSPORT_ETHERNET) == true && caps.hasCapability(NET_CAPABILITY_INTERNET)
    }

    override fun getLocalIpAddress(): String? {
        try {
            val linkAddresses = (connectivityManager.getLinkProperties(connectivityManager.activeNetwork) as LinkProperties).linkAddresses
            val ipAddressV4 = linkAddresses.firstOrNull {
                it.address.hostAddress?.contains(".") ?: false
            }?.address?.hostAddress
            return ipAddressV4
        } catch (exception: Exception) {
            Log.e(logName, exception.stackTraceToString())
        }

        return null
    }

    fun getNetworkState(): NetworkState = currentNetworkState

    private fun updateNetworkState() {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        currentNetworkState = if (capabilities != null) {
            NetworkState(
                hasInternet = capabilities.hasCapability(NET_CAPABILITY_INTERNET),
                networkTransportType = getTransportFromNetworkCapabilities(capabilities)
            )
        } else {
            NetworkState(false)
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
}