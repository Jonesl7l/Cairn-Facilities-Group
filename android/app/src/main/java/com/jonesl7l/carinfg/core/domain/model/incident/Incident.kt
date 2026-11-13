package com.jonesl7l.carinfg.core.domain.model.incident

import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import com.jonesl7l.carinfg.core.domain.model.sync.SyncStatus
import kotlin.time.Instant

data class Incident(
    val id: String,
    val referenceNumber: String?,
    val reporterId: String,
    val reportedAt: Instant?,
    val gpsLocation: GpsLocation?,
    val details: IncidentDetails,
    val location: IncidentLocation,
    val safety: IncidentSafety,
    val persons: List<Person>,
    val photos: List<Photo>,
    val additionalNotes: String?,
    val operatorFields: OperatorFields,
    val serverFields: ServerFields,
    val syncStatus: SyncStatus,
)
