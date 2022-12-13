package com.yavin.yavinandroidsdk.logger.ui

import android.content.Context
import android.util.Log
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.yavin.yavinandroidsdk.logger.YavinLogger
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import java.text.SimpleDateFormat
import java.util.*

object YavinLoggerUI {

    fun buildDatePicker(context: Context, yavinLogger: YavinLogger, callback: YavinLoggerUICallback): MaterialDatePicker<Long> {
        val files = yavinLogger.getLogsFiles(context)
        val filesName = mutableListOf<String>()
        var constraintsBuilder = CalendarConstraints.Builder()

        if (files.isNotEmpty()) {
            files.forEach { file ->
                val filename = file.name.substringBeforeLast(".")
                filesName.add(filename)
                Log.d("YavinLoggerUI", "filename: $filename")
            }

            val newestFileName = filesName.firstOrNull()
            val oldestFileName = filesName.lastOrNull()

            val dateFilenameFormatter = SimpleDateFormat(YavinLoggerConstants.DATE_FORMAT, Locale.US)
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

        datePicker.addOnPositiveButtonClickListener { date ->
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            utc.timeInMillis = date
            callback.onPositiveYavinLoggerDatePicker(utc.time)
        }

        return datePicker
    }

    interface YavinLoggerUICallback {
        fun onPositiveYavinLoggerDatePicker(selectedDate: Date)
    }
}