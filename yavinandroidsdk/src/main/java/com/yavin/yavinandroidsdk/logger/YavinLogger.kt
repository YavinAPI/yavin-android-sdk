package com.yavin.yavinandroidsdk.logger

import android.content.Context
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import java.io.File
import java.util.*

interface YavinLogger {
    fun init(config: YavinLoggerConfig, callback: YavinLoggerCallback)
    fun log(message: String)
    fun log(action: Action)

    fun getLogsFiles(context: Context): List<File>
    fun getLogsFile(context: Context, date: Date): File

    interface YavinLoggerCallback {
        fun onAppCrashed(exception: Throwable)
    }
}



