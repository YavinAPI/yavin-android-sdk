package com.yavin.yavinandroidsdk.logger.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class YavinLoggerUploadBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var yavinLogger: YavinLogger

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null || context == null) return

        if (intent.action == YavinLoggerConstants.ACTION_BROADCAST_UPLOAD_LOG) {
            yavinLogger.launchUploaderWorker(context)
        }
    }
}