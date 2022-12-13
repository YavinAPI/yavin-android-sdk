package com.yavin.yavinandroidsdk.logger

import android.content.Context
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import java.io.File

interface YavinLogger {
    fun init(config: YavinLoggerConfig, callback: YavinLoggerCallback)
    fun log(message: String)
    fun log(action: Action)

    fun getMostRecentLogsFile(context: Context): File?

    interface YavinLoggerCallback {
        fun onAppCrashed(exception: Throwable)
    }
}



