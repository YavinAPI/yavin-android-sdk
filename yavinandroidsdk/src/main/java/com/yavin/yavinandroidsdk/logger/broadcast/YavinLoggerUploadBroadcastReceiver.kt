package com.yavin.yavinandroidsdk.logger.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import dagger.hilt.android.AndroidEntryPoint
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class YavinLoggerUploadBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "YavinLoggerUploadBroadcastReceiver"
    }

    @Inject
    lateinit var yavinLogger: YavinLogger

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        if (intent.action == YavinLoggerConstants.ACTION_BROADCAST_UPLOAD_LOG) {
            val date = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(
                        YavinLoggerConstants.ACTION_BROADCAST_UPLOAD_LOG_ARG_DATE,
                        Date::class.java
                    )
                } else {
                    intent.getSerializableExtra(YavinLoggerConstants.ACTION_BROADCAST_UPLOAD_LOG_ARG_DATE) as Date
                }
            } catch (ex: Exception) {
                Date()
            }

            yavinLogger.log("$TAG: will launchUploaderWorker() with date: $date")

            date?.let { yavinLogger.launchUploaderWorker(context, it) }
        }
    }
}