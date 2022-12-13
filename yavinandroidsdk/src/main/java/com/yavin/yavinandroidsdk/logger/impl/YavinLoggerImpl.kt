package com.yavin.yavinandroidsdk.logger.impl

import android.content.Context
import com.yavin.yavinandroidsdk.files.YavinFilesManager
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import com.yavin.yavinandroidsdk.logger.exceptions.YavinLoggerNotInitializedException
import com.yavin.yavinandroidsdk.logger.utils.LogsUtils
import com.yavin.yavinandroidsdk.logger.utils.getCrashText
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class YavinLoggerImpl constructor(
    applicationContext: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val yavinFilesManager: YavinFilesManager
) : YavinLogger {

    companion object {
        private const val LOG_DIRECTORY = "logs"
        private const val DATE_FORMAT = "yyyy-MM-dd"

        /**
         * Format of the date in the header of the logs
         */
        const val DATETIME_LOGS_HEADER_FORMAT = "yyyy-MM-dd\' \'HH:mm:ss"
    }

    private var isInitialized = false

    private val dateFilenameFormatter = SimpleDateFormat(DATE_FORMAT, Locale.US)
    private val datetimeLogsHeaderFormatter = SimpleDateFormat(DATETIME_LOGS_HEADER_FORMAT, Locale.US)

    private val filename: String
        get() {
            val now = Date()
            return "${dateFilenameFormatter.format(now)}.txt"
        }

    private val logFile: File by lazy {
        yavinFilesManager.getFileFromDirectory(applicationContext, LOG_DIRECTORY, filename)
    }

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override fun init(config: YavinLoggerConfig, callback: YavinLogger.YavinLoggerCallback) {
        setCrashInterceptor(callback)

        val version = "${config.applicationVersionName} (${config.applicationVersionCode})"
        val initialLog = "\n    ====> New session: '${config.applicationName}' - $version ${Date()} <=====    \n"
        internalLog(initialLog, false)

        isInitialized = true
    }

    private fun setCrashInterceptor(callback: YavinLogger.YavinLoggerCallback) {
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            internalLog("Application crashed with following reason: ${exception.getCrashText()}", isCrash = true)
            callback.onAppCrashed(exception)

            defaultUncaughtExceptionHandler?.uncaughtException(thread, exception)
        }
    }

    override fun getMostRecentLogsFile(context: Context): File? {
        return yavinFilesManager.getMostRecentFromDirectory(context, LOG_DIRECTORY)
    }

    private fun internalLog(message: String, isCrash: Boolean) {
        val now = Date()
        val datetimeHeader = datetimeLogsHeaderFormatter.format(now)

        val callerFunction = LogsUtils.getCallerInfo(Thread.currentThread())

        val logHeader = if(isCrash) "$datetimeHeader: " else "$datetimeHeader: [$callerFunction]"

        var lineToLog = "$logHeader $message"
        lineToLog = lineToLog.replace("\n", "\n$logHeader")

        if(isCrash) {
            logFile.appendText("$lineToLog\n")
        } else {
            scope.launch {
                logFile.appendText("$lineToLog\n")
            }
        }
    }

    override fun log(message: String) {
        if (!isInitialized) {
            throw YavinLoggerNotInitializedException()
        }

        internalLog(message, false)
    }

    override fun log(action: Action) {
        if (!isInitialized) {
            throw YavinLoggerNotInitializedException()
        }

        internalLog(action.describeAction(), false)
    }
}
