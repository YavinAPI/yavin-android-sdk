package com.yavin.yavinandroidsdk.logger.exceptions

class YavinLoggerMissingImplementationException constructor(override val message: String = "YavinLogger is configured with '.registerNavControllerDestinationChangeListener()' but Activity is not implemented YavinLoggerNavigableActivity") : Exception(message)