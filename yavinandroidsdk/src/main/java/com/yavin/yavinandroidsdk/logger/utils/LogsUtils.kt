package com.yavin.yavinandroidsdk.logger.utils

object LogsUtils {

    // We take the fifth index because the caller function (apart from logger functions) is five functions deeper in the stacktrace tree.
    fun getCallerInfo(thread: Thread): String {
        val stackTrace = thread.stackTrace
        return buildCallerInfo(stackTrace)
    }

    /**
     * This function able log to determine the name of the calling function based on the stacktrace
     */
    private fun buildCallerInfo(stackTrace: Array<StackTraceElement>): String {
        return try {
            val callerFunctionDepth = 5
            val element = stackTrace[callerFunctionDepth]
            val fileName = element.className.substringAfterLast(".")
            val methodName = "${element.methodName}()"
            val line = element.lineNumber
            "$fileName[$methodName:$line]"
        } catch (e: Exception) {
            if (stackTrace.isNotEmpty()) stackTrace.last().methodName else "NoStackTrace"
        }
    }
}