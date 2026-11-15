package com.jonesl7l.carinfg.feature.incident.list

import kotlinx.datetime.LocalDate

sealed interface RelativeTime {
    data object JustNow : RelativeTime

    data class MinutesAgo(val minutes: Int) : RelativeTime

    data class HoursAgo(val hours: Int) : RelativeTime

    data object Yesterday : RelativeTime

    data class OnDate(val date: LocalDate) : RelativeTime
}
