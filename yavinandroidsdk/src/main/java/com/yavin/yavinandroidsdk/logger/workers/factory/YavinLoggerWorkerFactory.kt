package com.yavin.yavinandroidsdk.logger.workers.factory

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.repository.IYavinLoggerUploaderRepository
import com.yavin.yavinandroidsdk.logger.workers.YavinLoggerCleanerWorker
import com.yavin.yavinandroidsdk.logger.workers.YavinLoggerUploaderWorker

class YavinLoggerWorkerFactory(
    private val yavinLogger: YavinLogger,
    private val uploaderRepository: IYavinLoggerUploaderRepository
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            YavinLoggerCleanerWorker::class.java.name -> YavinLoggerCleanerWorker(appContext, workerParameters, yavinLogger)
            YavinLoggerUploaderWorker::class.java.name -> YavinLoggerUploaderWorker(appContext, workerParameters, yavinLogger, uploaderRepository)
            else ->
                // Return null, so that the base class can delegate to the default WorkerFactory.
                null
        }
    }
}