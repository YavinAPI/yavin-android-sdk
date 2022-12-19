package com.yavin.yavinandroidsdk.logger.impl

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.work.*
import com.yavin.yavinandroidsdk.files.YavinFilesManager
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.YavinLoggerNavigableActivity
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.config.YavinLoggerConfig
import com.yavin.yavinandroidsdk.logger.exceptions.YavinLoggerMissingImplementationException
import com.yavin.yavinandroidsdk.logger.exceptions.YavinLoggerNotInitializedException
import com.yavin.yavinandroidsdk.logger.utils.LogsUtils
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import com.yavin.yavinandroidsdk.logger.utils.getCrashText
import com.yavin.yavinandroidsdk.logger.workers.YavinLoggerCleanerWorker
import com.yavin.yavinandroidsdk.logger.workers.YavinLoggerUploaderWorker
import com.yavin.yavinandroidsdk.network.YavinConnectivityProvider
import com.yavin.yavinandroidsdk.network.YavinConnectivityProviderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class YavinLoggerImpl(
    applicationContext: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private val yavinFilesManager: YavinFilesManager
) : YavinLogger, YavinConnectivityProvider.ConnectivityStateListener {

    companion object {
        const val TAG_CLEANER_WORKER = "tag_yavin_logger_cleaner_worker"
        const val NAME_CLEANER_WORKER = "yavin_logger_cleaner_worker"
    }

    private var config: YavinLoggerConfig? = null

    private val dateFilenameFormatter = SimpleDateFormat(YavinLoggerConstants.DATE_FORMAT, Locale.US)
    private val datetimeLogsHeaderFormatter = SimpleDateFormat(YavinLoggerConstants.DATETIME_LOGS_HEADER_FORMAT, Locale.US)

    private val filename: String
        get() {
            val now = Date()
            return buildFilenameFromDate(now, "txt")
        }

    private val logFile: File by lazy {
        yavinFilesManager.getFileFromDirectory(applicationContext, YavinLoggerConstants.LOG_DIRECTORY, filename)
    }

    private val cleanerWorkerRequest = PeriodicWorkRequestBuilder<YavinLoggerCleanerWorker>(1, TimeUnit.DAYS)
        .addTag(TAG_CLEANER_WORKER)
        .build()

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    private var registerNavControllerDestinationChangedListener = false

    private val onDestinationChangedListener = NavController.OnDestinationChangedListener { controller, destination, arguments ->
        val label = destination.label?.toString() ?: applicationContext.resources.getResourceEntryName(destination.id)
        internalLog("Destination changed to screen with label \"$label\".", appendCaller = false, isCrash = false)
    }

    private val activityCallback = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            onActivityStateChanged(activity.localClassName, "CREATED")
        }

        override fun onActivityStarted(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "STARTED")
        }

        override fun onActivityResumed(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "RESUMED")

            if (registerNavControllerDestinationChangedListener) {
                if (activity is YavinLoggerNavigableActivity) {
                    activity.getNavController().addOnDestinationChangedListener(onDestinationChangedListener)
                } else {
                    throw YavinLoggerMissingImplementationException()
                }
            }
        }

        override fun onActivityPaused(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "PAUSED")

            if (registerNavControllerDestinationChangedListener) {
                if (activity is YavinLoggerNavigableActivity) {
                    activity.getNavController().removeOnDestinationChangedListener(onDestinationChangedListener)
                } else {
                    throw YavinLoggerMissingImplementationException()
                }
            }
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
        this.config = config
        val version = "${config.applicationVersionName} (${config.applicationVersionCode})"
        val initialLog = "\n    ====> New session: '${config.applicationName}' - $version ${Date()} <=====    \n"
        internalLog(initialLog, appendCaller = true, isCrash = false)
    }

    override fun registerCleanerWorker(context: Context) {
        checkInitialization()

        WorkManager
            .getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(NAME_CLEANER_WORKER, ExistingPeriodicWorkPolicy.REPLACE, cleanerWorkerRequest)
    }

    override fun getLoggerConfig(): YavinLoggerConfig {
        checkInitialization()
        return config!!
    }

    private fun checkInitialization() {
        if (config == null) {
            throw YavinLoggerNotInitializedException()
        }
    }

    override fun setCrashInterceptor(callback: YavinLogger.YavinLoggerCallback) {
        checkInitialization()

        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            internalLog("Application crashed with following reason: ${exception.getCrashText()}", appendCaller = false, isCrash = true)
            callback.onAppCrashed(exception)

            defaultUncaughtExceptionHandler?.uncaughtException(thread, exception)
        }
    }

    override fun registerActivityLifecycleCallbacks(application: Application) {
        checkInitialization()
        application.registerActivityLifecycleCallbacks(activityCallback)
    }

    override fun registerNavControllerDestinationChangeListener() {
        checkInitialization()
        registerNavControllerDestinationChangedListener = true
    }

    private fun onActivityStateChanged(activityName: String, state: String) {
        internalLog("Activity \"$activityName\" state is: $state", appendCaller = false, isCrash = false)
    }

    override fun registerConnectivityListener(context: Context) {
        checkInitialization()

        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityProvider: YavinConnectivityProvider = YavinConnectivityProviderImpl(cm, wm)
        connectivityProvider.addListener(this)
    }

    private fun buildFilenameFromDate(date: Date, extension: String): String {
        return "${dateFilenameFormatter.format(date)}.$extension"
    }

    override fun getLogsFiles(context: Context): List<File> {
        checkInitialization()
        return yavinFilesManager.getFilesFromDirectory(context, YavinLoggerConstants.LOG_DIRECTORY, true) ?: emptyList()
    }

    override fun getLogsFile(context: Context, date: Date): File {
        checkInitialization()
        val fileName = buildFilenameFromDate(date, "txt")
        return yavinFilesManager.getFileFromDirectory(context, YavinLoggerConstants.LOG_DIRECTORY, fileName)
    }

    override fun getArchivesFiles(context: Context): List<File> {
        checkInitialization()
        return yavinFilesManager.getFilesFromDirectory(context, YavinLoggerConstants.ARCHIVES_DIRECTORY, true) ?: emptyList()
    }

    override fun getArchivesFile(context: Context, date: Date): File {
        checkInitialization()
        val fileName = buildFilenameFromDate(date, "gz")
        return yavinFilesManager.getFileFromDirectory(context, YavinLoggerConstants.ARCHIVES_DIRECTORY, fileName)
    }

    override fun launchUploaderWorker(context: Context, date: Date): LiveData<List<WorkInfo>> {
        val workManager = WorkManager.getInstance(context.applicationContext)

        val requestName = YavinLoggerUploaderWorker.getWorkerTagName(date)
        val workRequest = YavinLoggerUploaderWorker.buildRequest(requestName, date)
        workManager.enqueueUniqueWork(requestName, ExistingWorkPolicy.KEEP, workRequest)

        return workManager.getWorkInfosForUniqueWorkLiveData(requestName)
    }

    private fun internalLog(message: String, appendCaller: Boolean, isCrash: Boolean) {
        val now = Date()
        val datetimeHeader = datetimeLogsHeaderFormatter.format(now)

        val callerFunction = LogsUtils.getCallerInfo(Thread.currentThread())

        val logHeader = if (isCrash) {
            "$datetimeHeader: "
        } else if (appendCaller) {
            "$datetimeHeader: $callerFunction"
        } else {
            "$datetimeHeader: [YL]"
        }

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
        internalLog(message, appendCaller = true, isCrash = false)
    }

    override fun log(action: Action) {
        checkInitialization()
        internalLog(action.describeAction(), appendCaller = true, isCrash = false)
    }

    override fun onConnectivityStateChange(state: YavinConnectivityProvider.NetworkState) {
        val networkType = when (state.networkTransportType) {
            NetworkCapabilities.TRANSPORT_WIFI -> "WIFI"
            NetworkCapabilities.TRANSPORT_ETHERNET -> "ETHERNET"
            NetworkCapabilities.TRANSPORT_CELLULAR -> "CELLULAR"
            else -> "UNKNOWN (${state.networkTransportType})"
        }
        internalLog("Connectivity changed: (type: $networkType, has Internet: ${state.hasInternet}).", appendCaller = false, isCrash = false)
    }
}
