package com.yavin.yavinandroidsdk

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import dagger.hilt.InstallIn
import dagger.hilt.android.EarlyEntryPoint
import dagger.hilt.android.EarlyEntryPoints
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider, YavinLogger.YavinLoggerCallback {

    @EarlyEntryPoint
    @InstallIn(SingletonComponent::class)
    interface ApplicationEarlyEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var yavinLogger: YavinLogger

    private fun initHiltInjection() {
        EarlyEntryPoints.get(this, ApplicationEarlyEntryPoint::class.java).apply {
            workerFactory = workerFactory()
        }
    }

    override fun getWorkManagerConfiguration(): Configuration =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        initHiltInjection()

        val yavinLoggerConfig = YavinLoggerConfig(
            BuildConfig.APPLICATION_ID,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            30
        )

        yavinLogger.init(yavinLoggerConfig)
        yavinLogger.setCrashInterceptor(this)
        yavinLogger.registerActivityLifecycleCallbacks(this)
        yavinLogger.registerNavControllerDestinationChangeListener()
        yavinLogger.registerConnectivityListener(this)
        yavinLogger.registerCleanerWorker(this)
    }

    override fun onAppCrashed(exception: Throwable) {
        // Send crash log by email if needed
    }
}