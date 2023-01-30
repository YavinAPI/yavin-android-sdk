package com.yavin.yavinandroidsdk.network

import android.net.Network

interface YavinConnectivityProvider {

    interface ConnectivityStateListener {
        fun onConnectivityStateChange(state: NetworkState)
    }

    fun getActiveNetwork(): Network?

    fun addListener(listener: ConnectivityStateListener)

    fun removeListener(listener: ConnectivityStateListener)

    fun hasInternet(): Boolean

    fun isWifiEnabled(): Boolean

    fun isWifiConnected(): Boolean

    fun isEthernetConnected(): Boolean

    fun isMobileDataConnected(): Boolean

    fun getLocalIpAddress(): String?

    data class NetworkState(
        val hasInternet: Boolean,
        val networkTransportType: Int? = null
    )
}