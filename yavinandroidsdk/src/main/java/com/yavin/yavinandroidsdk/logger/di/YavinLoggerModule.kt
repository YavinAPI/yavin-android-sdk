package com.yavin.yavinandroidsdk.logger.di

import android.content.Context
import com.yavin.yavinandroidsdk.files.YavinFilesManager
import com.yavin.yavinandroidsdk.files.impl.YavinFilesManagerImpl
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.impl.YavinLoggerImpl
import com.yavin.yavinandroidsdk.network.YavinConnectivityProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YavinLoggerModule {

    @Singleton
    @Provides
    fun provideYavinFilesManager(): YavinFilesManager {
        return YavinFilesManagerImpl()
    }

    @Singleton
    @Provides
    fun provideYavinLogger(
        @ApplicationContext context: Context,
        yavinFilesManager: YavinFilesManager,
        yavinConnectivityProvider: YavinConnectivityProvider
    ): YavinLogger {
        return YavinLoggerImpl(
            context,
            yavinFilesManager = yavinFilesManager,
            yavinConnectivityProvider = yavinConnectivityProvider
        )
    }
}