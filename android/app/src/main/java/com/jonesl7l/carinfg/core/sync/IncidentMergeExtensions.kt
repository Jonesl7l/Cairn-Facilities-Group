package com.jonesl7l.carinfg.core.sync

import com.jonesl7l.carinfg.core.data.mapper.toPhotoEntities
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto

// region IncidentEntity
fun IncidentEntity.keepingLocalBookkeeping(row: IncidentEntity): IncidentEntity = copy(
    localRevision = row.localRevision,
    localUpdatedAt = row.localUpdatedAt,
)

fun IncidentEntity.withServerFields(remote: IncidentDto): IncidentEntity = copy(
    referenceNumber = remote.referenceNumber,
    status = remote.status,
    assignedTo = remote.assignedTo,
    dueDate = remote.dueDate,
    rootCause = remote.rootCause,
    correctiveActions = remote.correctiveActions,
    regulatoryReportable = remote.regulatoryReportable,
    supervisorAcknowledged = remote.supervisorAcknowledged,
    createdAt = remote.createdAt,
    updatedAt = remote.updatedAt,
    serverVersion = remote.version,
    changeSeq = remote.changeSeq,
    deletedAt = remote.deletedAt ?: deletedAt,
    deletedBy = remote.deletedBy ?: deletedBy,
)
// endregion

// region IncidentDto
fun IncidentDto.toPhotoEntitiesKeepingPaths(localPhotos: List<PhotoEntity>): List<PhotoEntity> {
    val localPaths = localPhotos.associate { it.id to it.localPath }
    return toPhotoEntities().map { it.copy(localPath = localPaths[it.id]) }
}
// endregion
