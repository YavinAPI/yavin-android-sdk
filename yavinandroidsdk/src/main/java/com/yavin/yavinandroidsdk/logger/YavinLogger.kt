package com.yavin.yavinandroidsdk.logger

import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import com.yavin.yavinandroidsdk.logger.actions.Action
import java.io.File
import java.util.*

interface YavinLogger {
    fun init(application: Application, applicationName: String, applicationVersionName: String, applicationVersionCode: Int): YavinLogger
    fun setCrashInterceptor(callback: YavinLoggerCallback): YavinLogger
    fun setActivityLifecycleCallbacks(application: Application): YavinLogger
    fun setNavControllerDestinationChangeListener(): YavinLogger
    fun setConnectivityListener(useActivityLifecycle: Boolean): YavinLogger
    fun launchUploaderWorker(context: Context): LiveData<List<WorkInfo>>
    fun broadcastLogsUpload(context: Context)
    fun log(message: String)
    fun log(action: Action)
    fun share(callback: (File) -> Unit)
    fun clearLogs()

    interface YavinLoggerCallback {
        fun onAppCrashed(exception: Throwable)
    }
}