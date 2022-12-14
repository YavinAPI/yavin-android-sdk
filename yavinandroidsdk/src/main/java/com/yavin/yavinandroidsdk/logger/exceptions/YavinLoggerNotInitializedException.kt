package com.yavin.yavinandroidsdk.logger.exceptions

class YavinLoggerNotInitializedException constructor(override val message: String = "YavinLogger not initialized.") : Exception(message)