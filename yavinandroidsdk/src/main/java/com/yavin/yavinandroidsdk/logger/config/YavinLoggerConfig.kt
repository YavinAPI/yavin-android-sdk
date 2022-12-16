package com.yavin.yavinandroidsdk.logger.config

data class YavinLoggerConfig(
    val applicationName: String,
    val applicationVersionName: String,
    val applicationVersionCode: Int,
    val deleteAfterInDays: Int = 30
)