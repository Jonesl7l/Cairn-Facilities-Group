package com.jonesl7l.carinfg.core.domain.model.incident

import kotlin.time.Instant
import kotlinx.datetime.TimeZone

data class IncidentDetails(
    val title: String,
    val description: String,
    val occurredAt: Instant,
    val occurredTimeZone: TimeZone,
    val type: IncidentType?,
    val severity: Severity?,
)
