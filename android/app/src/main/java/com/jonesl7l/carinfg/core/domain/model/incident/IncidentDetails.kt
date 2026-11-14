package com.jonesl7l.carinfg.core.domain.model.incident

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

data class IncidentDetails(
    val title: String,
    val description: String,
    val occurredAt: Instant,
    /** shows occurredAt in the local time of where it happened */
    val occurredTimeZone: TimeZone,
    // nullable so drafts can save incomplete
    val type: IncidentType?,
    val severity: Severity?,
)
