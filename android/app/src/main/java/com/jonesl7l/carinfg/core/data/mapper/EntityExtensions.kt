package com.jonesl7l.carinfg.core.data.mapper

import com.jonesl7l.carinfg.core.database.entity.incident.GpsLocationColumns
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.incident.InjuryColumn
import com.jonesl7l.carinfg.core.database.entity.incident.PersonColumn
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.domain.model.incident.GpsLocation
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentDetails
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentLocation
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentSafety
import com.jonesl7l.carinfg.core.domain.model.incident.OperatorFields
import com.jonesl7l.carinfg.core.domain.model.incident.ServerFields
import com.jonesl7l.carinfg.core.domain.model.person.InjuryDetails
import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import com.jonesl7l.carinfg.core.domain.model.reference.Asset
import com.jonesl7l.carinfg.core.domain.model.reference.Building
import com.jonesl7l.carinfg.core.domain.model.reference.Floor
import com.jonesl7l.carinfg.core.domain.model.reference.Room
import com.jonesl7l.carinfg.core.domain.model.reference.Site
import com.jonesl7l.carinfg.core.domain.model.sync.SyncStatus
import kotlinx.datetime.IllegalTimeZoneException
import kotlinx.datetime.TimeZone
import timber.log.Timber

// region IncidentWithPhotos
fun IncidentWithPhotos.toDomain(): Incident = Incident(
    id = incident.id,
    referenceNumber = incident.referenceNumber,
    reporterId = incident.reporterId,
    reportedAt = incident.reportedAt,
    gpsLocation = incident.gpsLocation?.toDomain(),
    details = incident.toDetails(),
    location = incident.toLocation(),
    safety = incident.toSafety(),
    persons = incident.persons.map { it.toDomain() },
    photos = photos.sortedBy { it.position }.map { it.toDomain() },
    additionalNotes = incident.additionalNotes,
    operatorFields = incident.toOperatorFields(),
    serverFields =
        ServerFields(
            createdAt = incident.createdAt,
            updatedAt = incident.updatedAt,
            deletedAt = incident.deletedAt,
            deletedBy = incident.deletedBy,
        ),
    syncStatus =
        SyncStatus(
            state = incident.syncState,
            conflictReason = incident.conflictReason,
            attempts = incident.syncAttempts,
            lastError = incident.lastSyncError,
        ),
)
// endregion

// region IncidentEntity
private fun IncidentEntity.toDetails() = IncidentDetails(
    title = title,
    description = description,
    occurredAt = occurredAt,
    occurredTimeZone = toTimeZone(id = occurredTimeZone),
    type = incidentType?.let {
        WireValueMapper.toIncidentType(
            raw = it,
            otherDescription = incidentTypeOther,
        )
    },
    severity = severity?.let(WireValueMapper::toSeverity),
)

private fun IncidentEntity.toLocation() = IncidentLocation(
    siteId = siteId,
    buildingId = buildingId,
    floorId = floorId,
    roomId = roomId,
    unlistedLocation = unlistedLocation,
    exactSpot = exactSpot,
    assetId = assetId,
    equipmentOutOfService = equipmentOutOfService,
)

private fun IncidentEntity.toSafety() = IncidentSafety(
    injuryOccurred = injuryOccurred,
    hazardStillPresent = hazardStillPresent,
    immediateActions =
        immediateActions
            .map {
                WireValueMapper.toImmediateAction(
                    raw = it,
                    otherDescription = immediateActionsOther,
                )
            }.toSet(),
)

private fun IncidentEntity.toOperatorFields() = OperatorFields(
    status = WireValueMapper.toIncidentStatus(raw = status),
    assignedTo = assignedTo,
    dueDate = dueDate,
    rootCause = rootCause,
    correctiveActions = correctiveActions,
    regulatoryReportable = regulatoryReportable,
    supervisorAcknowledged = supervisorAcknowledged,
)

private fun toTimeZone(id: String): TimeZone = try {
    TimeZone.of(zoneId = id)
} catch (error: IllegalTimeZoneException) {
    Timber.w(error, "toTimeZone: unknown time zone %s, using UTC", id)
    TimeZone.UTC
}
// endregion

// region GpsLocationColumns
private fun GpsLocationColumns.toDomain() = GpsLocation(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
)
// endregion

// region PersonColumn
private fun PersonColumn.toDomain() = Person(
    id = id,
    name = name,
    type = WireValueMapper.toPersonType(raw = personType),
    role = WireValueMapper.toPersonRole(raw = role),
    department = department,
    position = position,
    company = company,
    contactNumber = contactNumber,
    injury = injury?.toDomain(),
)
// endregion

// region InjuryColumn
private fun InjuryColumn.toDomain() = InjuryDetails(
    nature = nature,
    bodyPart = bodyPart,
    treatment = WireValueMapper.toTreatment(raw = treatment),
    notes = notes,
)
// endregion

// region PhotoEntity
fun PhotoEntity.toDomain() = Photo(
    id = id,
    incidentId = incidentId,
    position = position,
    localPath = localPath,
    remoteUrl = remoteUrl,
    uploadState = uploadState,
)
// endregion

// region SiteEntity
fun SiteEntity.toDomain() = Site(
    id = id,
    name = name,
)
// endregion

// region BuildingEntity
fun BuildingEntity.toDomain() = Building(
    id = id,
    siteId = siteId,
    name = name,
)
// endregion

// region FloorEntity
fun FloorEntity.toDomain() = Floor(
    id = id,
    buildingId = buildingId,
    name = name,
)
// endregion

// region RoomEntity
fun RoomEntity.toDomain() = Room(
    id = id,
    floorId = floorId,
    name = name,
)
// endregion

// region AssetEntity
fun AssetEntity.toDomain() = Asset(
    id = id,
    roomId = roomId,
    name = name,
    code = code,
)
// endregion
