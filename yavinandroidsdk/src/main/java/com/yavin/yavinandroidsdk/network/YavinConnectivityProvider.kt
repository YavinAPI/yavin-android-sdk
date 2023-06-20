package com.yavin.yavinandroidsdk.network

import android.net.Network

interface YavinConnectivityProvider {

    interface ConnectivityStateListener {
        fun onConnectivityStateChange(state: NetworkState)
    }

    fun getActiveNetwork(): Network?

    fun addListener(listener: ConnectivityStateListener)

    fun removeListener(listener: ConnectivityStateListener)

    fun hasInternetCapability(): Boolean

    fun isWifiEnabled(): Boolean

    fun isWifiConnected(): Boolean

    fun isEthernetConnected(): Boolean

    fun isMobileDataConnected(): Boolean

    fun getLocalIpAddress(): String?

    suspend fun testRealInternetConnection(): Boolean

    data class NetworkState(
        val hasInternetCapability: Boolean,
        val networkTransportType: Int? = null
    )
}