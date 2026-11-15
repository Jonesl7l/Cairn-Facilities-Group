package com.jonesl7l.carinfg.feature.incident.list

import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

object RelativeTimeHelper {
    private val justNowWindow = 1.minutes
    private val minutesWindow = 1.hours
    private val yesterday = DatePeriod(days = 1)

    fun describe(
        time: Instant,
        now: Instant,
        timeZone: TimeZone,
    ): RelativeTime {
        val elapsed = now - time
        val today = now.toLocalDateTime(timeZone = timeZone).date
        val day = time.toLocalDateTime(timeZone = timeZone).date
        return when {
            elapsed < justNowWindow -> RelativeTime.JustNow
            elapsed < minutesWindow -> RelativeTime.MinutesAgo(minutes = elapsed.inWholeMinutes.toInt())
            day == today -> RelativeTime.HoursAgo(hours = elapsed.inWholeHours.toInt())
            day == today.minus(period = yesterday) -> RelativeTime.Yesterday
            else -> RelativeTime.OnDate(date = day)
        }
    }
}
