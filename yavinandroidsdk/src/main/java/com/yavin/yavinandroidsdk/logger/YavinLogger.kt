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
    fun setActivityLifecycleCallbacks(): YavinLogger
    fun setNavControllerDestinationChangeListener(): YavinLogger
    fun setConnectivityListener(useActivityLifecycle: Boolean): YavinLogger
    fun registerCleanerWorker(deleteAfterInDays: Int = 30): YavinLogger
    fun getNumberOfDaysBeforeCleaning(): Int

    fun launchUploaderWorker(context: Context, date: Date): LiveData<List<WorkInfo>>
    fun broadcastLogsUpload(context: Context, date: Date)

    fun log(message: String)
    fun log(action: Action)

    fun getLogsFiles(context: Context): List<File>
    fun getLogsFile(context: Context, date: Date): File

    fun getArchivesFiles(context: Context): List<File>
    fun getArchivesFile(context: Context, date: Date): File

    interface YavinLoggerCallback {
        fun onAppCrashed(exception: Throwable)
    }
}