package com.yavin.yavinandroidsdk.logger.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.yavin.yavinandroidsdk.R
import com.yavin.yavinandroidsdk.files.utils.YavinFilesUtils
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.repository.IYavinLoggerUploaderRepository
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

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

        private const val KEY_INPUT_DATA_DATE = TAG_UPLOAD_PREFIX + "input_date"
        const val KEY_OUTPUT_DATA_RESULT = TAG_UPLOAD_PREFIX + "result"
        const val KEY_OUTPUT_DATA_FILE_NOT_FOUND = TAG_UPLOAD_PREFIX + "FILE_NOT_FOUND"

        private val dateFilenameFormatter = SimpleDateFormat(YavinLoggerConstants.DATE_FORMAT, Locale.US)

        private val uploaderWorkerBuilder = OneTimeWorkRequestBuilder<YavinLoggerUploaderWorker>()
            .addTag(TAG_COMMON)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)

        fun getWorkerTagName(date: Date): String {
            val formattedDate = dateFilenameFormatter.format(date)
            return "$TAG_UPLOAD_PREFIX$formattedDate"
        }

        fun buildRequest(tag: String, date: Date): OneTimeWorkRequest {
            val formattedDate = dateFilenameFormatter.format(date)
            return uploaderWorkerBuilder
                .addTag(formattedDate)
                .addTag(tag)
                .setInputData(
                    Data.Builder().putString(KEY_INPUT_DATA_DATE, formattedDate)
                        .build()
                )
                .build()
        }
    }

    override suspend fun doWork() = withContext(Dispatchers.IO) {
        val date = dateFilenameFormatter.parse(inputData.getString(KEY_INPUT_DATA_DATE)!!)!!
        val fileToUpload = yavinLogger.getLogsFile(applicationContext, date)

        return@withContext if (fileToUpload.exists() && fileToUpload.isFile) {
            repository.uploadFile(applicationContext, fileToUpload)
        } else {
            val compressedFile = yavinLogger.getArchivesFile(applicationContext, date)
            if(compressedFile.exists() && compressedFile.isFile) {
                val destinationFile = yavinLogger.getLogsFile(applicationContext, date)
                YavinFilesUtils.uncompressFile(compressedFile, destinationFile)
                repository.uploadFile(applicationContext, destinationFile)
            } else {
                val outputData = Data.Builder().apply {
                    putString(KEY_OUTPUT_DATA_RESULT, KEY_OUTPUT_DATA_FILE_NOT_FOUND)
                }.build()

                Result.failure(outputData)
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(NOTIFICATION_ID, createNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.yavin_logger_uploader_channel_name)
            val descriptionText = applicationContext.getString(R.string.yavin_logger_uploader_channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(YAVIN_NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(applicationContext, YAVIN_NOTIFICATION_CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.yavin_logger_uploader_notification_title))
            .setContentText(applicationContext.getString(R.string.yavin_logger_uploader_notification_message))
            .build()
    }
}