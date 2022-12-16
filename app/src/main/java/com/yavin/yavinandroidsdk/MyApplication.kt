package com.yavin.yavinandroidsdk

import android.app.Application
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import com.yavin.yavinandroidsdk.logger.workers.factory.YavinLoggerWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltAndroidApp
class MyApplication : Application(), Configuration.Provider, YavinLogger.YavinLoggerCallback {

    @Inject
    lateinit var yavinLogger: YavinLogger

    @Inject
    lateinit var uploaderRepository: MyLoggerUploaderRepository

    override fun getWorkManagerConfiguration(): Configuration {
        val myWorkerFactory = DelegatingWorkerFactory()
        myWorkerFactory.addFactory(YavinLoggerWorkerFactory(yavinLogger, uploaderRepository))

        return Configuration.Builder()
            .setWorkerFactory(myWorkerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

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

        // Upload today's log file
        yavinLogger.launchUploaderWorker(this, Date())
    }

    override fun onAppCrashed(exception: Throwable) {
        // Send crash log by email if needed
    }
}