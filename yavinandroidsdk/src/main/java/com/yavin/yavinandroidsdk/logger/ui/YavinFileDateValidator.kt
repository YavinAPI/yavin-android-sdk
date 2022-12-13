package com.yavin.yavinandroidsdk.logger.ui

import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import com.yavin.yavinandroidsdk.logger.utils.YavinLoggerConstants
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.*

@Parcelize
class YavinFileDateValidator(private val filesName: List<String>) : DateValidator {

    @IgnoredOnParcel
    private val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    @IgnoredOnParcel
    private val dateFilenameFormatter = SimpleDateFormat(YavinLoggerConstants.DATE_FORMAT, Locale.US)

    override fun isValid(date: Long): Boolean {
        utc.timeInMillis = date
        val formattedDate = dateFilenameFormatter.format(utc.time)
        return filesName.contains(formattedDate)
    }
}