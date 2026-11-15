package com.jonesl7l.carinfg.core.data.mapper

import com.jonesl7l.carinfg.core.database.entity.incident.GpsLocationColumns
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.InjuryColumn
import com.jonesl7l.carinfg.core.database.entity.incident.PersonColumn
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.domain.model.incident.GpsLocation
import com.jonesl7l.carinfg.core.domain.model.incident.ImmediateAction
import com.jonesl7l.carinfg.core.domain.model.incident.Incident
import com.jonesl7l.carinfg.core.domain.model.incident.IncidentType
import com.jonesl7l.carinfg.core.domain.model.person.InjuryDetails
import com.jonesl7l.carinfg.core.domain.model.person.Person
import com.jonesl7l.carinfg.core.domain.model.photo.Photo
import kotlin.time.Instant

// region Incident
fun Incident.toEntity(
    existing: IncidentEntity?,
    localUpdatedAt: Instant,
): IncidentEntity = IncidentEntity(
    id = id,
    referenceNumber = referenceNumber,
    reporterId = reporterId,
    reportedAt = reportedAt,
    gpsLocation = gpsLocation?.toColumns(),
    title = details.title,
    description = details.description,
    occurredAt = details.occurredAt,
    occurredTimeZone = details.occurredTimeZone.id,
    incidentType = details.type?.let(WireValueMapper::toWire),
    incidentTypeOther = (details.type as? IncidentType.Other)?.description,
    severity = details.severity?.let(WireValueMapper::toWire),
    severityRank = details.severity?.rank ?: 0,
    siteId = location.siteId,
    buildingId = location.buildingId,
    floorId = location.floorId,
    roomId = location.roomId,
    unlistedLocation = location.unlistedLocation,
    exactSpot = location.exactSpot,
    assetId = location.assetId,
    equipmentOutOfService = location.equipmentOutOfService,
    injuryOccurred = safety.injuryOccurred,
    hazardStillPresent = safety.hazardStillPresent,
    immediateActions = safety.immediateActions.map(WireValueMapper::toWire),
    immediateActionsOther = safety.immediateActions.otherText(),
    persons = persons.map { it.toColumn() },
    additionalNotes = additionalNotes,
    status = WireValueMapper.toWire(status = operatorFields.status),
    assignedTo = operatorFields.assignedTo,
    dueDate = operatorFields.dueDate,
    rootCause = operatorFields.rootCause,
    correctiveActions = operatorFields.correctiveActions,
    regulatoryReportable = operatorFields.regulatoryReportable,
    supervisorAcknowledged = operatorFields.supervisorAcknowledged,
    createdAt = serverFields.createdAt,
    updatedAt = serverFields.updatedAt,
    serverVersion = existing?.serverVersion,
    changeSeq = existing?.changeSeq,
    deletedAt = serverFields.deletedAt,
    deletedBy = serverFields.deletedBy,
    syncState = syncStatus.state,
    conflictReason = syncStatus.conflictReason,
    previousSyncState = existing?.previousSyncState,
    undoDeadline = existing?.undoDeadline,
    localRevision = existing?.localRevision ?: 0,
    lastSyncError = syncStatus.lastError,
    syncAttempts = syncStatus.attempts,
    localUpdatedAt = localUpdatedAt,
)
// endregion

// region ImmediateAction
private fun Set<ImmediateAction>.otherText(): String? = filterIsInstance<ImmediateAction.Other>().firstOrNull()?.description
// endregion

// region GpsLocation
private fun GpsLocation.toColumns() = GpsLocationColumns(
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = accuracyMeters,
)
// endregion

// region Person
private fun Person.toColumn() = PersonColumn(
    id = id,
    name = name,
    personType = WireValueMapper.toWire(type = type),
    role = WireValueMapper.toWire(role = role),
    department = department,
    position = position,
    company = company,
    contactNumber = contactNumber,
    injury = injury?.toColumn(),
)
// endregion

// region InjuryDetails
private fun InjuryDetails.toColumn() = InjuryColumn(
    nature = nature,
    bodyPart = bodyPart,
    treatment = WireValueMapper.toWire(treatment = treatment),
    notes = notes,
)
// endregion

// region Photo
fun Photo.toEntity() = PhotoEntity(
    id = id,
    incidentId = incidentId,
    position = position,
    localPath = localPath,
    remoteUrl = remoteUrl,
    uploadState = uploadState,
)
// endregion
