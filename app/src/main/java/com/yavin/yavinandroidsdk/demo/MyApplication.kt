package com.yavin.yavinandroidsdk.demo

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import androidx.work.DelegatingWorkerFactory
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.utils.getCrashText
import com.yavin.yavinandroidsdk.logger.workers.factory.YavinLoggerWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import java.util.*
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

        yavinLogger.init(this, BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
            .setCrashInterceptor(this)
            .setActivityLifecycleCallbacks()
            .setNavControllerDestinationChangeListener()
            .setConnectivityListener()
            .registerCleanerWorker(30)

        // Upload today's log file
        yavinLogger.launchUploaderWorker(this, Date())
    }

    override fun onAppCrashed(exception: Throwable) {
        // Send crash log by email if needed
        Log.e("MyApplication", exception.getCrashText())
    }
}