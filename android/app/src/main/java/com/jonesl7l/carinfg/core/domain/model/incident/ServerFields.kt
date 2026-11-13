package com.jonesl7l.carinfg.core.domain.model.incident

import kotlin.time.Instant

data class ServerFields(
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val deletedAt: Instant?,
    val deletedBy: String?,
)
