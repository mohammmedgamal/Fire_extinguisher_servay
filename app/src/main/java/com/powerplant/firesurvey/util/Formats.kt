package com.powerplant.firesurvey.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object Formats {
    private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Extinguishers are expected to be checked at least this often. */
    const val INSPECTION_INTERVAL_DAYS = 30

    fun dateTime(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateTimeFormat)

    fun date(millis: Long): String =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(dateFormat)

    fun daysAgo(millis: Long, now: Long = System.currentTimeMillis()): Long =
        TimeUnit.MILLISECONDS.toDays(now - millis)

    fun relative(millis: Long): String = when (val d = daysAgo(millis)) {
        0L -> "today"
        1L -> "yesterday"
        else -> "$d days ago"
    }

    fun isOverdue(lastMillis: Long?): Boolean =
        lastMillis == null || daysAgo(lastMillis) >= INSPECTION_INTERVAL_DAYS
}
