package com.jonesl7l.carinfg.feature.incident.list

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test

class RelativeTimeHelperTest {
    @Test
    fun `under a minute is just now`() {
        assertThat(describe(time = now - 30.seconds)).isEqualTo(RelativeTime.JustNow)
    }

    @Test
    fun `under an hour counts minutes`() {
        assertThat(describe(time = now - 40.minutes)).isEqualTo(RelativeTime.MinutesAgo(minutes = 40))
    }

    @Test
    fun `earlier today counts hours`() {
        assertThat(describe(time = now - 3.hours)).isEqualTo(RelativeTime.HoursAgo(hours = 3))
    }

    @Test
    fun `the day before is yesterday`() {
        assertThat(describe(time = now - 20.hours)).isEqualTo(RelativeTime.Yesterday)
    }

    @Test
    fun `anything older shows its date`() {
        val expected = RelativeTime.OnDate(
            date = LocalDate(
                year = 2026,
                month = 11,
                day = 12,
            ),
        )

        assertThat(describe(time = now - 3.days)).isEqualTo(expected)
    }

    private fun describe(time: Instant) = RelativeTimeHelper.describe(
        time = time,
        now = now,
        timeZone = TimeZone.UTC,
    )

    private companion object {
        val now: Instant = Instant.parse(input = "2026-11-15T12:00:00Z")
    }
}
