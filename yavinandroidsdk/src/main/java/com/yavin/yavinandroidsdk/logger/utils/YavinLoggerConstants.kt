package com.yavin.yavinandroidsdk.logger.utils

import java.io.File

object YavinLoggerConstants {
    const val LOG_DIRECTORY = "logs"
    val ARCHIVES_DIRECTORY = LOG_DIRECTORY + File.separator + "archives"

    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DATETIME_LOGS_HEADER_FORMAT = "yyyy-MM-dd\' \'HH:mm:ss"

    // Match the intent-filter action in AndroidManifest
    const val ACTION_BROADCAST_UPLOAD_LOG = "com.yavin.logger.broadcast.action.ACTION_BROADCAST_UPLOAD_LOG"
    const val ACTION_BROADCAST_UPLOAD_LOG_ARG_DATE = "com.yavin.logger.broadcast.action.ACTION_BROADCAST_UPLOAD_LOG.date"
}