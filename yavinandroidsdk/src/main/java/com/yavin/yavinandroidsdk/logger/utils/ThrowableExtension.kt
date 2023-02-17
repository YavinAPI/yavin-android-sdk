package com.yavin.yavinandroidsdk.logger.utils

import java.lang.reflect.InvocationTargetException

fun Throwable.getCrashText(): String {
    var arr = this.stackTrace
    var report = "$this  \n"
    for (i in arr.indices) {
        report += "   at ${arr[i]} \n"
    }
    val cause = this.cause
    if (cause != null) {
        report += "$cause ".trimIndent()
        arr = cause.stackTrace
        for (i in arr.indices) {
            report += "   at ${arr[i]} \n"
        }
        report += "\n"
        kotlin.runCatching {
            (cause as InvocationTargetException).targetException.stackTrace.forEach {
                report += "   at ${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber})\n"
            }
        }
    }

    return report
}