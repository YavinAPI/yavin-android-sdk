package com.yavin.yavinandroidsdk.logger.impl

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.yavin.yavinandroidsdk.files.YavinFilesManager
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import com.yavin.yavinandroidsdk.logger.exceptions.YavinLoggerNotInitializedException
import com.yavin.yavinandroidsdk.logger.utils.LogsUtils
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import com.yavin.yavinandroidsdk.logger.utils.getCrashText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class YavinLoggerImpl constructor(
    applicationContext: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val yavinFilesManager: YavinFilesManager
) : YavinLogger {

    private var isInitialized = false

    private val dateFilenameFormatter = SimpleDateFormat(YavinLoggerConstants.DATE_FORMAT, Locale.US)
    private val datetimeLogsHeaderFormatter = SimpleDateFormat(YavinLoggerConstants.DATETIME_LOGS_HEADER_FORMAT, Locale.US)

    private val filename: String
        get() {
            val now = Date()
            return buildFilenameFromDate(now)
        }

    private val logFile: File by lazy {
        yavinFilesManager.getFileFromDirectory(applicationContext, YavinLoggerConstants.LOG_DIRECTORY, filename)
    }

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    private val activityCallback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            onActivityStateChanged(activity.localClassName, "CREATED")
        }

        override fun onActivityStarted(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "STARTED")
        }

        override fun onActivityResumed(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "RESUMED")
        }

        override fun onActivityPaused(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "PAUSED")
        }

        override fun onActivityStopped(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "STOPPED")
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            onActivityStateChanged(activity.localClassName, "SAVE_INSTANCE_STATE")
        }

        override fun onActivityDestroyed(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "DESTROYED")
        }
    }

    override fun init(config: YavinLoggerConfig) {
        val version = "${config.applicationVersionName} (${config.applicationVersionCode})"
        val initialLog = "\n    ====> New session: '${config.applicationName}' - $version ${Date()} <=====    \n"
        internalLog(initialLog, false)

        isInitialized = true
    }

    private fun checkInitialization() {
        if (!isInitialized) {
            throw YavinLoggerNotInitializedException()
        }
    }

    override fun setCrashInterceptor(callback: YavinLogger.YavinLoggerCallback) {
        checkInitialization()

        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            internalLog("Application crashed with following reason: ${exception.getCrashText()}", isCrash = true)
            callback.onAppCrashed(exception)

            defaultUncaughtExceptionHandler?.uncaughtException(thread, exception)
        }
    }

    override fun registerActivityLifecycleCallbacks(application: Application) {
        checkInitialization()
        application.registerActivityLifecycleCallbacks(activityCallback)
    }

    private fun onActivityStateChanged(activityName: String, state: String) {
        log("$activityName $state")
    }

    private fun buildFilenameFromDate(date: Date): String {
        return "${dateFilenameFormatter.format(date)}.txt"
    }

    override fun getLogsFiles(context: Context): List<File> {
        checkInitialization()
        return yavinFilesManager.getFilesFromDirectory(context, YavinLoggerConstants.LOG_DIRECTORY, true) ?: emptyList()
    }

    override fun getLogsFile(context: Context, date: Date): File {
        checkInitialization()
        val fileName = buildFilenameFromDate(date)
        return yavinFilesManager.getFileFromDirectory(context, YavinLoggerConstants.LOG_DIRECTORY, fileName)
    }

    private fun internalLog(message: String, isCrash: Boolean) {
        val now = Date()
        val datetimeHeader = datetimeLogsHeaderFormatter.format(now)

        val callerFunction = LogsUtils.getCallerInfo(Thread.currentThread())

        val logHeader = if (isCrash) "$datetimeHeader: " else "$datetimeHeader: $callerFunction"

        var lineToLog = "$logHeader $message"
        lineToLog = lineToLog.replace("\n", "\n$logHeader")

        if (isCrash) {
            logFile.appendText("$lineToLog\n")
        } else {
            scope.launch {
                logFile.appendText("$lineToLog\n")
            }
        }
    }

    override fun log(message: String) {
        checkInitialization()
        internalLog(message, false)
    }

    override fun log(action: Action) {
        checkInitialization()
        internalLog(action.describeAction(), false)
    }
}
