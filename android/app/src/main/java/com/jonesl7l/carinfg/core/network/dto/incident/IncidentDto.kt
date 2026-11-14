package com.jonesl7l.carinfg.core.network.dto.incident

import com.jonesl7l.carinfg.core.network.dto.person.PersonDto
import com.jonesl7l.carinfg.core.network.dto.photo.PhotoDto
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class IncidentDto(
    val id: String,
    val referenceNumber: String,
    val reporterId: String,
    val reportedAt: Instant,
    val gpsLocation: GpsLocationDto? = null,
    val title: String,
    val description: String,
    val occurredAt: Instant,
    val occurredTimeZone: String,
    val incidentType: String,
    val incidentTypeOther: String? = null,
    val severity: String,
    val siteId: String,
    val buildingId: String? = null,
    val floorId: String? = null,
    val roomId: String? = null,
    val unlistedLocation: String? = null,
    val exactSpot: String? = null,
    val assetId: String? = null,
    val equipmentOutOfService: Boolean = false,
    val injuryOccurred: Boolean,
    val hazardStillPresent: Boolean,
    val immediateActions: List<String> = emptyList(),
    val immediateActionsOther: String? = null,
    val persons: List<PersonDto> = emptyList(),
    val photos: List<PhotoDto> = emptyList(),
    val additionalNotes: String? = null,
    val status: String,
    val assignedTo: String? = null,
    val dueDate: LocalDate? = null,
    val rootCause: String? = null,
    val correctiveActions: String? = null,
    val regulatoryReportable: Boolean? = null,
    val supervisorAcknowledged: Boolean? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Int,
    val changeSeq: Long,
    val deletedAt: Instant? = null,
    val deletedBy: String? = null,
)
