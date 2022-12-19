package com.yavin.yavinandroidsdk.logger.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.yavin.yavinandroidsdk.files.utils.YavinFilesUtils
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class YavinLoggerCleanerWorker constructor(
    context: Context,
    workerParams: WorkerParameters,
    private val yavinLogger: YavinLogger
) : Worker(context.applicationContext, workerParams) {

    private val dateFilenameFormatter = SimpleDateFormat(YavinLoggerConstants.DATE_FORMAT, Locale.US)

    override fun doWork(): Result {
        cleanLogsFiles()
        cleanArchivesFiles()

        return Result.success()
    }

    private fun cleanLogsFiles() {
        val files = yavinLogger.getLogsFiles(applicationContext)

        files.forEach { file ->
            val fileNameWithoutExtension = file.nameWithoutExtension
            val fileDate = dateFilenameFormatter.parse(fileNameWithoutExtension)

            if (fileDate != null) {
                Log.d("YavinLoggerCleanerWorker", "Handling cleaning of log file: ${file.name}")
                when {
                    shouldDeleteFile(fileDate) -> {
                        if (file.isFile && file.exists()) {
                            file.delete()
                            Log.d("YavinLoggerCleanerWorker", "Log file ${file.name} deleted")
                        }
                    }

                    shouldArchiveFile(file, fileDate) -> {
                        if (file.isFile && file.exists()) {
                            archiveFile(file, fileDate)
                            file.delete()
                            Log.d("YavinLoggerCleanerWorker", "Log file ${file.name} archived")
                        }
                    }

                    else -> {
                        Log.d("YavinLoggerCleanerWorker", "Log file ${file.name} not outdated")
                    }
                }
            }
        }
    }

    private fun cleanArchivesFiles() {
        val files = yavinLogger.getArchivesFiles(applicationContext)

        files.forEach { file ->
            val fileNameWithoutExtension = file.nameWithoutExtension
            val fileDate = dateFilenameFormatter.parse(fileNameWithoutExtension)

            if (fileDate != null) {
                Log.d("YavinLoggerCleanerWorker", "Handling cleaning of archive file: ${file.name}")
                when {
                    shouldDeleteFile(fileDate) -> {
                        if (file.isFile && file.exists()) {
                            file.delete()
                            Log.d("YavinLoggerCleanerWorker", "Archive file ${file.name} deleted")
                        }
                    }

                    else -> {
                        Log.d("YavinLoggerCleanerWorker", "Archive file ${file.name} not outdated")
                    }
                }
            }
        }
    }

    private fun daysToMillis(days: Int): Long {
        return days * 24 * 60 * 60 * 1000L
    }

    private fun shouldDeleteFile(fileDate: Date): Boolean {
        val now = Calendar.getInstance()
        now.set(Calendar.HOUR_OF_DAY, 0)
        now.set(Calendar.MINUTE, 0)
        now.set(Calendar.SECOND, 0)
        now.set(Calendar.MILLISECOND, 0)
        return (now.timeInMillis - fileDate.time) > daysToMillis(yavinLogger.getNumberOfDaysBeforeCleaning())
    }

    private fun shouldArchiveFile(file: File, fileDate: Date): Boolean {
        return if (file.extension == "gz") {
            false
        } else {
            val now = Calendar.getInstance()
            // If it's not today
            !isSameDay(now.time, fileDate)
        }
    }

    private fun archiveFile(file: File, fileDate: Date) {
        val archiveFile = yavinLogger.getArchivesFile(applicationContext, fileDate)
        YavinFilesUtils.compressFile(file, archiveFile)
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val calendar1 = Calendar.getInstance()
        calendar1.time = date1
        val calendar2 = Calendar.getInstance()
        calendar2.time = date2
        return calendar1[Calendar.YEAR] == calendar2[Calendar.YEAR] &&
            calendar1[Calendar.MONTH] == calendar2[Calendar.MONTH] &&
            calendar1[Calendar.DAY_OF_MONTH] == calendar2[Calendar.DAY_OF_MONTH]
    }
}