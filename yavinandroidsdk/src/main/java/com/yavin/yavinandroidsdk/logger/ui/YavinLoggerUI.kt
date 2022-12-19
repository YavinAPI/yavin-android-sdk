package com.yavin.yavinandroidsdk.logger.ui

import android.content.Context
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.ui.validator.YavinFileDateValidator
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import java.text.SimpleDateFormat
import java.util.*

object YavinLoggerUI {

    fun buildDatePicker(context: Context, yavinLogger: YavinLogger, callback: YavinLoggerUICallback): MaterialDatePicker<Long> {
        val dateFilenameFormatter = SimpleDateFormat(YavinLoggerConstants.DATE_FORMAT, Locale.US)

        val logsFiles = yavinLogger.getLogsFiles(context)
        val archivesFiles = yavinLogger.getArchivesFiles(context)

        // Key = fileName, Value = Boolean if file is archived or not
        val filesName = mutableMapOf<String, Boolean>()
        var constraintsBuilder = CalendarConstraints.Builder()

        logsFiles.forEach { file ->
            val filename = file.nameWithoutExtension
            filesName[filename] = false
        }

        archivesFiles.forEach { file ->
            val filename = file.nameWithoutExtension
            filesName[filename] = true
        }

        if (filesName.isNotEmpty()) {
            val sorted = filesName.toSortedMap(compareBy { dateFilenameFormatter.parse(it) })

            val newestFileName = sorted.lastKey()
            val oldestFileName = sorted.firstKey()

            val newest = dateFilenameFormatter.parse(newestFileName!!)?.time
            val oldest = dateFilenameFormatter.parse(oldestFileName!!)?.time

            if (newest != null) {
                constraintsBuilder = constraintsBuilder.setEnd(newest)
            }

            if (oldest != null) {
                constraintsBuilder = constraintsBuilder.setStart(oldest)
            }
        }

        constraintsBuilder = constraintsBuilder
            .setValidator(YavinFileDateValidator(filesName))

        val datePicker = MaterialDatePicker.Builder
            .datePicker()
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { timeInMillis ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeInMillis
            callback.onYavinLoggerFileSelected(calendar.time)
        }

        return datePicker
    }

    interface YavinLoggerUICallback {
        fun onYavinLoggerFileSelected(date: Date)
    }
}