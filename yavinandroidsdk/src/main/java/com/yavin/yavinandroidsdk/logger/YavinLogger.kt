package com.yavin.yavinandroidsdk.logger

import android.app.Application
import android.content.Context
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import java.io.File
import java.util.*

interface YavinLogger {
    fun init(config: YavinLoggerConfig)
    fun setCrashInterceptor(callback: YavinLoggerCallback)
    fun registerActivityLifecycleCallbacks(application: Application)

    fun log(message: String)
    fun log(action: Action)

    fun getLogsFiles(context: Context): List<File>
    fun getLogsFile(context: Context, date: Date): File

    interface YavinLoggerCallback {
        fun onAppCrashed(exception: Throwable)
    }
}



