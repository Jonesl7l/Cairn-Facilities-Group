package com.jonesl7l.carinfg.core.data.mapper

import com.jonesl7l.carinfg.core.database.entity.incident.GpsLocationColumns
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.InjuryColumn
import com.jonesl7l.carinfg.core.database.entity.incident.PersonColumn
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.network.dto.incident.GpsLocationDto
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.dto.person.InjuryDto
import com.jonesl7l.carinfg.core.network.dto.person.PersonDto
import com.jonesl7l.carinfg.core.network.dto.reference.AssetDto
import com.jonesl7l.carinfg.core.network.dto.reference.BuildingDto
import com.jonesl7l.carinfg.core.network.dto.reference.FloorDto
import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import com.jonesl7l.carinfg.core.network.dto.reference.RoomDto
import com.jonesl7l.carinfg.core.network.dto.reference.SiteDto
import com.jonesl7l.carinfg.core.network.dto.user.UserDto

// region ReferenceDataDto
fun ReferenceDataDto.toEntities(): ReferenceEntities = ReferenceEntities(
    sites = sites.map { it.toEntity() },
    buildings = buildings.map { it.toEntity() },
    floors = floors.map { it.toEntity() },
    rooms = rooms.map { it.toEntity() },
    assets = assets.map { it.toEntity() },
    technicians = technicians.map { it.toTechnicianEntity() },
)
// endregion

// region SiteDto
private fun SiteDto.toEntity() = SiteEntity(
    id = id,
    name = name,
)
// endregion

// region BuildingDto
private fun BuildingDto.toEntity() = BuildingEntity(
    id = id,
    siteId = siteId,
    name = name,
)
// endregion

// region FloorDto
private fun FloorDto.toEntity() = FloorEntity(
    id = id,
    buildingId = buildingId,
    name = name,
)
// endregion

// region RoomDto
private fun RoomDto.toEntity() = RoomEntity(
    id = id,
    floorId = floorId,
    name = name,
)
// endregion

// region AssetDto
private fun AssetDto.toEntity() = AssetEntity(
    id = id,
    roomId = roomId,
    name = name,
    code = code,
)
// endregion

// region UserDto
private fun UserDto.toTechnicianEntity() = TechnicianEntity(
    id = id,
    username = username,
    displayName = displayName,
)

fun UserDto.toDomain() = User(
    id = id,
    username = username,
    displayName = displayName,
    role = WireValueMapper.toUserRole(raw = role),
)
// endregion

// region IncidentDto
fun IncidentDto.toEntity() = IncidentEntity(
    id = id,
    referenceNumber = referenceNumber,
    reporterId = reporterId,
    reportedAt = reportedAt,
    gpsLocation = gpsLocation?.toColumns(),
    title = title,
    description = description,
    occurredAt = occurredAt,
    occurredTimeZone = occurredTimeZone,
    incidentType = incidentType,
    incidentTypeOther = incidentTypeOther,
    severity = severity,
    severityRank = WireValueMapper.toSeverity(raw = severity).rank,
    siteId = siteId,
    buildingId = buildingId,
    floorId = floorId,
    roomId = roomId,
    unlistedLocation = unlistedLocation,
    exactSpot = exactSpot,
    assetId = assetId,
    equipmentOutOfService = equipmentOutOfService,
    injuryOccurred = injuryOccurred,
    hazardStillPresent = hazardStillPresent,
    immediateActions = immediateActions,
    immediateActionsOther = immediateActionsOther,
    persons = persons.map { it.toColumn() },
    additionalNotes = additionalNotes,
    status = status,
    assignedTo = assignedTo,
    dueDate = dueDate,
    rootCause = rootCause,
    correctiveActions = correctiveActions,
    regulatoryReportable = regulatoryReportable,
    supervisorAcknowledged = supervisorAcknowledged,
    createdAt = createdAt,
    updatedAt = updatedAt,
    serverVersion = version,
    changeSeq = changeSeq,
    deletedAt = deletedAt,
    deletedBy = deletedBy,
    syncState = SyncState.SYNCED,
    conflictReason = null,
    previousSyncState = null,
    undoDeadline = null,
    localRevision = 0,
    lastSyncError = null,
    syncAttempts = 0,
    localUpdatedAt = updatedAt,
)

fun IncidentDto.toPhotoEntities(): List<PhotoEntity> = photos.mapIndexed { index, photo ->
    PhotoEntity(
        id = photo.id,
        incidentId = id,
        position = index,
        localPath = null,
        remoteUrl = photo.url,
        uploadState = PhotoUploadState.UPLOADED,
    )
}
// endregion

// region GpsLocationDto
private fun GpsLocationDto.toColumns() = GpsLocationColumns(
    latitude = lat,
    longitude = lng,
    accuracyMeters = accuracyMeters,
)
// endregion

// region PersonDto
private fun PersonDto.toColumn() = PersonColumn(
    id = id,
    name = name,
    personType = personType,
    role = role,
    department = department,
    position = position,
    company = company,
    contactNumber = contactNumber,
    injury = injury?.toColumn(),
)
// endregion

// region InjuryDto
private fun InjuryDto.toColumn() = InjuryColumn(
    nature = nature,
    bodyPart = bodyPart,
    treatment = treatment,
    notes = notes,
)
// endregion
