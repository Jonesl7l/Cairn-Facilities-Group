package com.jonesl7l.carinfg.core.database.entity.incident

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "incidents",
    indices = [Index(value = ["syncState"])],
)
data class IncidentEntity(
    @PrimaryKey val id: String,
    val referenceNumber: String?,
    val reporterId: String,
    val reportedAt: Instant?,
    @Embedded(prefix = "gps_") val gpsLocation: GpsLocationColumns?,
    val title: String,
    val description: String,
    val occurredAt: Instant,
    val occurredTimeZone: String,
    val incidentType: String?,
    val incidentTypeOther: String?,
    val severity: String?,
    val severityRank: Int,
    val siteId: String?,
    val buildingId: String?,
    val floorId: String?,
    val roomId: String?,
    val unlistedLocation: String?,
    val exactSpot: String?,
    val assetId: String?,
    val equipmentOutOfService: Boolean,
    val injuryOccurred: Boolean?,
    val hazardStillPresent: Boolean?,
    val immediateActions: List<String>,
    val immediateActionsOther: String?,
    val persons: List<PersonColumn>,
    val additionalNotes: String?,
    val status: String,
    val assignedTo: String?,
    val dueDate: LocalDate?,
    val rootCause: String?,
    val correctiveActions: String?,
    val regulatoryReportable: Boolean?,
    val supervisorAcknowledged: Boolean?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
    val serverVersion: Int?,
    val changeSeq: Long?,
    val deletedAt: Instant?,
    val deletedBy: String?,
    val syncState: SyncState,
    val conflictReason: ConflictReason?,
    val previousSyncState: SyncState?,
    val undoDeadline: Instant?,
    val localRevision: Long,
    val lastSyncError: String?,
    val syncAttempts: Int,
    val localUpdatedAt: Instant,
)
