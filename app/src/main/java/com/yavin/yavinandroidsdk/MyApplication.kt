package com.yavin.yavinandroidsdk

import android.app.Application
import android.util.Log
import com.yavin.yavinandroidsdk.files.impl.YavinFilesManagerImpl
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import com.yavin.yavinandroidsdk.logger.impl.YavinLoggerImpl

class MyApplication: Application(), YavinLogger.YavinLoggerCallback {

   private val yavinFilesManager = YavinFilesManagerImpl()
   private lateinit var yavinLogger: YavinLogger

    override fun onCreate() {
        super.onCreate()

        yavinLogger = YavinLoggerImpl(this, yavinFilesManager = yavinFilesManager)

        val yavinLoggerConfig = YavinLoggerConfig(
            BuildConfig.APPLICATION_ID,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        yavinLogger.init(yavinLoggerConfig, this)
    }

    fun logger(): YavinLogger {
        return yavinLogger
    }

    override fun onAppCrashed(exception: Throwable) {
        val file = yavinLogger.getMostRecentLogsFile(this)
        file?.forEachLine {
            Log.d("File", it)
        }
    }
}