package com.jonesl7l.carinfg.core.network.dto.incident

import com.jonesl7l.carinfg.core.network.dto.person.PersonDto
import com.jonesl7l.carinfg.core.network.dto.photo.PhotoDto
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class IncidentWriteRequest(
    val id: String,
    val reporterId: String,
    val reportedAt: Instant,
    val gpsLocation: GpsLocationDto?,
    val title: String,
    val description: String,
    val occurredAt: Instant,
    val occurredTimeZone: String,
    val incidentType: String,
    val incidentTypeOther: String?,
    val severity: String,
    val siteId: String,
    val buildingId: String?,
    val floorId: String?,
    val roomId: String?,
    val unlistedLocation: String?,
    val exactSpot: String?,
    val assetId: String?,
    val equipmentOutOfService: Boolean,
    val injuryOccurred: Boolean,
    val hazardStillPresent: Boolean,
    val immediateActions: List<String>,
    val immediateActionsOther: String?,
    val persons: List<PersonDto>,
    val photos: List<PhotoDto>,
    val additionalNotes: String?,
)
