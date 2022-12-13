package com.yavin.yavinandroidsdk.logger.utils

import java.lang.reflect.InvocationTargetException

fun Throwable.getCrashText(): String {
    var arr = this.stackTrace
    var report = "$this  \n\n"
    report += "--------- Stack trace ---------\n\n"
    for (i in arr.indices) {
        report += "    ${arr[i]} \n"
    }
    report += "-------------------------------\n\n"
    report += "--------- Cause ---------\n\n"
    val cause = this.cause
    if (cause != null) {
        report += "$cause ".trimIndent()
        arr = cause.stackTrace
        for (i in arr.indices) {
            report += "    ${arr[i]} \n"
        }
        report += "\n"
        kotlin.runCatching {
            (cause as InvocationTargetException).targetException.stackTrace.forEach {
                report += "    ${it.methodName} ${it.className} line number : ${it.lineNumber}"
                report += "    ${it.javaClass} \n"
                report += "    ${it.fileName} \n"
                report += "\n"
            }
        }
    }
    report += "-------------------------------\n\n"

    return report
}