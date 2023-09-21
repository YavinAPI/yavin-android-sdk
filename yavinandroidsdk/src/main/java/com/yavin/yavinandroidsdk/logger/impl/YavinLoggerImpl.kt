package com.yavin.yavinandroidsdk.logger.impl

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.navigation.NavController
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.yavin.yavinandroidsdk.files.YavinFilesManager
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.YavinLoggerNavigableActivity
import com.yavin.yavinandroidsdk.logger.actions.Action
import com.yavin.yavinandroidsdk.logger.utils.LogsUtils
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import com.yavin.yavinandroidsdk.logger.utils.getCrashText
import com.yavin.yavinandroidsdk.logger.workers.YavinLoggerUploaderWorker
import com.yavin.yavinandroidsdk.network.YavinConnectivityProvider
import io.getstream.log.Priority
import io.getstream.log.StreamLog
import io.getstream.log.streamLog
import io.getstream.logging.file.FileStreamLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class YavinLoggerImpl(
    applicationContext: Context,
    private val yavinFilesManager: YavinFilesManager,
    private val yavinConnectivityProvider: YavinConnectivityProvider
) : YavinLogger, YavinConnectivityProvider.ConnectivityStateListener {

    private lateinit var fileLogger: FileStreamLogger

    private val dateFormatter = SimpleDateFormat(YavinLoggerConstants.DATETIME_FORMAT, Locale.US)

    private var defaultUncaughtExceptionHandler: Thread.UncaughtExceptionHandler? = null

    private var registerNavControllerDestinationChangedListener = false

    private val logName = this::class.java.simpleName

    private val onDestinationChangedListener =
        NavController.OnDestinationChangedListener { _, destination, _ ->
            val label =
                destination.label?.toString() ?: applicationContext.resources.getResourceEntryName(
                    destination.id
                )
            internalLog("Destination changed to screen with label \"$label\".")
        }

    private var activityLifecycleState: String? = null

    private var useActivityLifecycleForConnectivityChecks: Boolean = false

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
                    activity.getNavController()
                        .addOnDestinationChangedListener(onDestinationChangedListener)
                } else {
                    Log.i(
                        "YavinLogger",
                        "${activity.localClassName} is not implementing YavinLoggerNavigableActivity"
                    )
                }
            }

            if (useActivityLifecycleForConnectivityChecks) {
                Log.d(logName, "Activity resumed - start listening for connectivity changes")
                yavinConnectivityProvider.addListener(this@YavinLoggerImpl)
            }
        }

        override fun onActivityPaused(activity: Activity) {
            onActivityStateChanged(activity.localClassName, "PAUSED")

            if (registerNavControllerDestinationChangedListener) {
                if (activity is YavinLoggerNavigableActivity) {
                    activity.getNavController()
                        .removeOnDestinationChangedListener(onDestinationChangedListener)
                } else {
                    Log.i(
                        "YavinLogger",
                        "${activity.localClassName} is not implementing YavinLoggerNavigableActivity"
                    )
                }
            }

            if (useActivityLifecycleForConnectivityChecks) {
                Log.d(logName, "Activity paused - stop listening for connectivity changes")
                yavinConnectivityProvider.removeListener(this@YavinLoggerImpl)
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

    override fun init(
        application: Application,
        applicationName: String,
        applicationVersionName: String,
        applicationVersionCode: Int
    ): YavinLogger {
        val logsDir =
            yavinFilesManager.getDirectory(application, YavinLoggerConstants.LOG_DIRECTORY)

        val fileLoggerConfig = FileStreamLogger.Config(
            maxLogSize = 4 * 1024 * 1024,
            filesDir = logsDir, // an internal file directory
            externalFilesDir = logsDir, // an external file directory. This is an optional.
            app = FileStreamLogger.Config.App( // application information.
                versionCode = applicationVersionCode.toLong(),
                versionName = applicationVersionName
            ),
            device = FileStreamLogger.Config.Device( // device information
                model = "%s %s".format(Build.MANUFACTURER, Build.DEVICE),
                androidApiLevel = Build.VERSION.SDK_INT
            )
        )
        fileLogger = FileStreamLogger(fileLoggerConfig)

        StreamLog.setValidator { priority, _ ->
            priority.level >= Priority.DEBUG.level
        }

        StreamLog.install(fileLogger)

        return this
    }

    override fun setCrashInterceptor(callback: YavinLogger.YavinLoggerCallback): YavinLogger {
        defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            internalLog(exception.getCrashText())
            callback.onAppCrashed(exception)

            defaultUncaughtExceptionHandler?.uncaughtException(thread, exception)
        }

        return this
    }

    override fun setActivityLifecycleCallbacks(application: Application): YavinLogger {
        application.registerActivityLifecycleCallbacks(activityCallback)
        return this
    }

    override fun setNavControllerDestinationChangeListener(): YavinLogger {
        registerNavControllerDestinationChangedListener = true
        return this
    }

    private fun onActivityStateChanged(activityName: String, state: String) {
        activityLifecycleState = state
        internalLog("Activity \"$activityName\" state is: $state")
    }

    override fun setConnectivityListener(useActivityLifecycle: Boolean): YavinLogger {
        Log.d(
            logName,
            "setConnectivityListener() use activityLifecycle=$useActivityLifecycle"
        )
        useActivityLifecycleForConnectivityChecks = useActivityLifecycle
        if (!useActivityLifecycle || activityLifecycleState == "RESUMED") {
            yavinConnectivityProvider.addListener(this)
        }

        return this
    }

    override fun share(callback: (File) -> Unit) {
        fileLogger.share {
            callback(it)
        }
    }

    override fun clearLogs() {
        fileLogger.clear()
    }

    override fun launchUploaderWorker(context: Context): LiveData<List<WorkInfo>> {
        val workManager = WorkManager.getInstance(context.applicationContext)

        val tag = dateFormatter.format(Date())
        val requestName = YavinLoggerUploaderWorker.getWorkerTagName(tag)
        val workRequest = YavinLoggerUploaderWorker.buildRequest(requestName, tag)
        workManager.enqueueUniqueWork(requestName, ExistingWorkPolicy.KEEP, workRequest)

        return workManager.getWorkInfosForUniqueWorkLiveData(requestName)
    }

    override fun broadcastLogsUpload(context: Context) {
        Intent().also { intent ->
            intent.action = YavinLoggerConstants.ACTION_BROADCAST_UPLOAD_LOG
            context.sendBroadcast(intent)
        }
    }

    private fun internalLog(message: String) {
        try {
            streamLog(tag = LogsUtils.getCallerInfo(Thread.currentThread())) { message }
        } catch (ex: Throwable) {
            ex.printStackTrace()
            Log.e("YavinLogger", "Crashed caused by: ${ex.message}")
        }
    }

    override fun log(message: String) {
        internalLog(message)
    }

    override fun log(action: Action) {
        internalLog(action.describeAction())
    }

    override fun onConnectivityStateChange(state: YavinConnectivityProvider.NetworkState) {
        val networkType = when (state.networkTransportType) {
            NetworkCapabilities.TRANSPORT_WIFI -> "WIFI"
            NetworkCapabilities.TRANSPORT_ETHERNET -> "ETHERNET"
            NetworkCapabilities.TRANSPORT_CELLULAR -> "CELLULAR"
            else -> "UNKNOWN (${state.networkTransportType})"
        }
        internalLog(
            "Connectivity changed: (type: $networkType, has Internet: ${state.hasInternetCapability})."
        )
    }
}
