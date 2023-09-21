package com.yavin.yavinandroidsdk.logger.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkerParameters
import com.yavin.yavinandroidsdk.R
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.repository.IYavinLoggerUploaderRepository
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class YavinLoggerUploaderWorker constructor(
    context: Context,
    workerParams: WorkerParameters,
    private val yavinLogger: YavinLogger,
    private val repository: IYavinLoggerUploaderRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val YAVIN_NOTIFICATION_CHANNEL_ID = "yavin_logger_uploader_channel_id"
        private const val NOTIFICATION_ID = 42

        private const val TAG_COMMON = "yavin_logger_uploader"
        private const val TAG_UPLOAD_PREFIX = TAG_COMMON + "_"

        const val KEY_OUTPUT_DATA_RESULT = TAG_UPLOAD_PREFIX + "result"
        const val KEY_OUTPUT_DATA_RETRIEVED_FILE_FAILURE =
            TAG_UPLOAD_PREFIX + "RETRIEVED_FILE_FAILURE"

        private val dateFilenameFormatter =
            SimpleDateFormat(YavinLoggerConstants.DATE_FORMAT, Locale.US)

        private val uploaderWorkerBuilder = OneTimeWorkRequestBuilder<YavinLoggerUploaderWorker>()
            .addTag(TAG_COMMON)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)

        fun getWorkerTagName(tag: String): String {
            return "$TAG_UPLOAD_PREFIX$tag"
        }

        fun buildRequest(tag: String, dateTag: String): OneTimeWorkRequest {
            return uploaderWorkerBuilder
                .addTag(dateTag)
                .addTag(tag)
                .build()
        }
    }

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<File?>()

        yavinLogger.share { file ->
            val date = Date()
            val newFilename = "${dateFilenameFormatter.format(date)}.txt"
            val newFile = File(file.parentFile, newFilename)

            if (newFile.exists()) {
                newFile.delete()
            }

            if (file.renameTo(newFile)) {
                deferred.complete(newFile)
            } else {
                deferred.complete(null)
            }
        }

        val fileToUpload = deferred.await() ?: kotlin.run {
            val outputData = Data.Builder().apply {
                putString(KEY_OUTPUT_DATA_RESULT, KEY_OUTPUT_DATA_RETRIEVED_FILE_FAILURE)
            }.build()
            return@withContext Result.failure(outputData)
        }

        val uploadResult = repository.uploadFile(applicationContext, fileToUpload)

        // Remove fileToUpload because it is a copy of internal logs file
        if (fileToUpload.exists()) {
            fileToUpload.delete()
        }

        if (uploadResult is Result.Success) {
            yavinLogger.clearLogs()
        }

        return@withContext uploadResult
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.yavin_logger_uploader_channel_name)
            val descriptionText =
                applicationContext.getString(R.string.yavin_logger_uploader_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel =
                NotificationChannel(YAVIN_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(applicationContext, YAVIN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.yavin_logger_uploader_notification_title))
            .setContentText(applicationContext.getString(R.string.yavin_logger_uploader_notification_message))
            .setSmallIcon(R.drawable.ic_file_upload)
            .build()
    }
}