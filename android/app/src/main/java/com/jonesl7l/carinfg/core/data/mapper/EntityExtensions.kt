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
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentListItem
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
import com.jonesl7l.carinfg.core.network.dto.incident.DeleteIncidentRequest
import com.jonesl7l.carinfg.core.network.dto.incident.GpsLocationDto
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentWriteRequest
import com.jonesl7l.carinfg.core.network.dto.person.InjuryDto
import com.jonesl7l.carinfg.core.network.dto.person.PersonDto
import com.jonesl7l.carinfg.core.network.dto.photo.PhotoDto
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

fun IncidentWithPhotos.toWriteRequest(): IncidentWriteRequest? {
    val reportedAt = incident.reportedAt ?: return null
    val incidentType = incident.incidentType ?: return null
    val severity = incident.severity ?: return null
    val siteId = incident.siteId ?: return null
    val injuryOccurred = incident.injuryOccurred ?: return null
    val hazardStillPresent = incident.hazardStillPresent ?: return null
    return IncidentWriteRequest(
        id = incident.id,
        reporterId = incident.reporterId,
        reportedAt = reportedAt,
        gpsLocation = incident.gpsLocation?.toDto(),
        title = incident.title,
        description = incident.description,
        occurredAt = incident.occurredAt,
        occurredTimeZone = incident.occurredTimeZone,
        incidentType = incidentType,
        incidentTypeOther = incident.incidentTypeOther,
        severity = severity,
        siteId = siteId,
        buildingId = incident.buildingId,
        floorId = incident.floorId,
        roomId = incident.roomId,
        unlistedLocation = incident.unlistedLocation,
        exactSpot = incident.exactSpot,
        assetId = incident.assetId,
        equipmentOutOfService = incident.equipmentOutOfService,
        injuryOccurred = injuryOccurred,
        hazardStillPresent = hazardStillPresent,
        immediateActions = incident.immediateActions,
        immediateActionsOther = incident.immediateActionsOther,
        persons = incident.persons.map { it.toDto() },
        photos = photos.sortedBy { it.position }.mapNotNull { it.toUploadedDto() },
        additionalNotes = incident.additionalNotes,
    )
}
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

fun IncidentEntity.toListItem(
    siteName: String?,
    buildingName: String?,
) = IncidentListItem(
    id = id,
    title = title,
    referenceNumber = referenceNumber,
    reporterId = reporterId,
    status = WireValueMapper.toIncidentStatus(raw = status),
    severity = severity?.let(WireValueMapper::toSeverity),
    syncState = syncState,
    syncAttempts = syncAttempts,
    occurredAt = occurredAt,
    localUpdatedAt = localUpdatedAt,
    siteName = siteName,
    buildingName = buildingName,
)

fun IncidentEntity.toDeleteRequest(): DeleteIncidentRequest? {
    val deletedAt = deletedAt ?: return null
    val deletedBy = deletedBy ?: return null
    return DeleteIncidentRequest(
        deletedAt = deletedAt,
        deletedBy = deletedBy,
    )
}
// endregion

// region GpsLocationColumns
private fun GpsLocationColumns.toDomain() = GpsLocation(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
)

private fun GpsLocationColumns.toDto() = GpsLocationDto(
    lat = latitude,
    lng = longitude,
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

private fun PersonColumn.toDto() = PersonDto(
    id = id,
    name = name,
    personType = personType,
    role = role,
    department = department,
    position = position,
    company = company,
    contactNumber = contactNumber,
    injury = injury?.toDto(),
)
// endregion

// region InjuryColumn
private fun InjuryColumn.toDomain() = InjuryDetails(
    nature = nature,
    bodyPart = bodyPart,
    treatment = WireValueMapper.toTreatment(raw = treatment),
    notes = notes,
)

private fun InjuryColumn.toDto() = InjuryDto(
    nature = nature,
    bodyPart = bodyPart,
    treatment = treatment,
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

private fun PhotoEntity.toUploadedDto(): PhotoDto? = remoteUrl?.let {
    PhotoDto(
        id = id,
        url = it,
    )
}
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
