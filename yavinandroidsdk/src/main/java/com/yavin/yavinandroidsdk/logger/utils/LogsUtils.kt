package com.yavin.yavinandroidsdk.logger.utils

object LogsUtils {

    fun getCallerInfo(thread: Thread): String {
        val stackTrace = thread.stackTrace
        return buildCallerInfo(stackTrace)
    }

    /**
     * This function able log to determine the name of the calling function based on the stacktrace.
     * We take the fifth index because the caller function (apart from logger functions) is five functions deeper in the stacktrace tree.
     */
    private fun buildCallerInfo(stackTrace: Array<StackTraceElement>): String {
        return try {
            val callerFunctionDepth = 5
            val element = stackTrace[callerFunctionDepth]
            val className = element.className.substringAfterLast(".")
            val methodName = "${element.methodName}()"
            val line = element.lineNumber
            "$className.$methodName:$line"
        } catch (e: Exception) {
            if (stackTrace.isNotEmpty()) stackTrace.last().methodName else "NoStackTrace"
        }
    }
}