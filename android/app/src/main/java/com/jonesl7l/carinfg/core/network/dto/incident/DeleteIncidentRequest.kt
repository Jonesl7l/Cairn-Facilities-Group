package com.jonesl7l.carinfg.core.network.dto.incident

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DeleteIncidentRequest(
    val deletedAt: Instant,
    val deletedBy: String,
)
