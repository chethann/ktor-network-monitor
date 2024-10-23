package io.github.chethann.network.monitor.view

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object TimeDisplayUtils {

    fun getReadableTime(timestamp: Long): String {
        val currentDateTime = getCurrentDateTimeFromTimestamp(timestamp)

        // Format it manually to a readable string (e.g., "2024-10-21 13:45:30")
        val year = currentDateTime.year
        val month = currentDateTime.monthNumber.toString().padStart(2, '0')  // Ensures two digits for month
        val day = currentDateTime.dayOfMonth.toString().padStart(2, '0')     // Ensures two digits for day
        val hour = currentDateTime.hour.toString().padStart(2, '0')          // Ensures two digits for hour
        val minute = currentDateTime.minute.toString().padStart(2, '0')      // Ensures two digits for minute
        val second = currentDateTime.second.toString().padStart(2, '0')      // Ensures two digits for second

        if (isToday(currentDateTime)) {
            return "$hour:$minute:$second"
        } else {
            return "$year-$month-$day $hour:$minute:$second"
        }
    }

    private fun isToday(dateTime: LocalDateTime, timeZone: TimeZone = TimeZone.currentSystemDefault()): Boolean {
        // Get current date in the same time zone
        val currentDateTime = Clock.System.now().toLocalDateTime(timeZone)

        // Compare year, month, and day of the given dateTime and the current date
        return dateTime.year == currentDateTime.year &&
                dateTime.monthNumber == currentDateTime.monthNumber &&
                dateTime.dayOfMonth == currentDateTime.dayOfMonth
    }

    private fun getCurrentDateTimeFromTimestamp(timestamp: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime {
        // Convert the timestamp to an Instant (assuming the timestamp is in milliseconds)
        val instant = Instant.fromEpochMilliseconds(timestamp)

        // Convert the Instant to a LocalDateTime in the specified time zone
        return instant.toLocalDateTime(timeZone)
    }
}