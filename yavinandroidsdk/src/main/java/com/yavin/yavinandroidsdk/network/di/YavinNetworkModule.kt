package com.yavin.yavinandroidsdk.network.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import com.yavin.yavinandroidsdk.network.YavinConnectivityProvider
import com.yavin.yavinandroidsdk.network.YavinConnectivityProviderImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YavinNetworkModule {

    @Singleton
    @Provides
    fun provideConnectivityProvider(@ApplicationContext context: Context): YavinConnectivityProvider {
        val cm =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return YavinConnectivityProviderImpl(cm, wm)
    }
}