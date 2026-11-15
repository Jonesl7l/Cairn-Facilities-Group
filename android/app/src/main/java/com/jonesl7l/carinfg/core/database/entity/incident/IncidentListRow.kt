package com.jonesl7l.carinfg.core.database.entity.incident

import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlin.time.Instant

data class IncidentListRow(
    val id: String,
    val title: String,
    val referenceNumber: String?,
    val reporterId: String,
    val status: String,
    val severity: String?,
    val syncState: SyncState,
    val syncAttempts: Int,
    val occurredAt: Instant,
    val localUpdatedAt: Instant,
    val siteName: String?,
    val buildingName: String?,
)
