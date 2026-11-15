package com.jonesl7l.carinfg.core.sync.push

import com.jonesl7l.carinfg.core.data.mapper.toEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.sync.keepingLocalBookkeeping
import com.jonesl7l.carinfg.core.sync.toPhotoEntitiesKeepingPaths
import com.jonesl7l.carinfg.core.sync.withServerFields

object PushCommitHelper {
    fun commit(
        current: IncidentWithPhotos,
        remote: IncidentDto,
        basedOnRevision: Long,
    ): PushCommit {
        val row = current.incident
        if (row.localRevision != basedOnRevision) {
            return keepPending(
                current = current,
                remote = remote,
            )
        }
        return PushCommit(
            entity = remote.toEntity().keepingLocalBookkeeping(row = row),
            replacementPhotos = remote.toPhotoEntitiesKeepingPaths(localPhotos = current.photos),
        )
    }

    private fun keepPending(
        current: IncidentWithPhotos,
        remote: IncidentDto,
    ): PushCommit {
        val row = current.incident
        val syncState = if (row.syncState == SyncState.PENDING_CREATE) SyncState.PENDING_UPDATE else row.syncState
        return PushCommit(
            entity =
                row.withServerFields(remote = remote).copy(
                    syncState = syncState,
                    lastSyncError = null,
                    syncAttempts = 0,
                ),
            replacementPhotos = null,
        )
    }
}
