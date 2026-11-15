package com.jonesl7l.carinfg.core.sync.store

import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.sync.SyncCounts
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import com.jonesl7l.carinfg.core.sync.push.PushCommit
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSyncLocalStore : SyncLocalStore {
    var meta = SyncMetaEntity()
    val incidents = mutableMapOf<String, IncidentWithPhotos>()
    val resets = mutableListOf<String>()
    val prunes = mutableListOf<Pair<String, Instant>>()
    val failures = mutableListOf<Pair<String, String>>()
    val syncCounts = MutableStateFlow(SyncCounts())

    override suspend fun getSyncMeta(): SyncMetaEntity = meta

    override fun observeSyncCounts(): Flow<SyncCounts> = syncCounts

    override suspend fun getIncidents(ids: List<String>): Map<String, IncidentWithPhotos> = incidents.filterKeys { it in ids }

    override suspend fun applyPullPage(
        actions: List<PullAction>,
        meta: SyncMetaEntity,
    ) {
        actions.forEach { action ->
            when (action) {
                PullAction.Skip -> Unit

                is PullAction.Upsert -> {
                    val photos = action.replacementPhotos ?: incidents[action.entity.id]?.photos.orEmpty()
                    incidents[action.entity.id] = IncidentWithPhotos(
                        incident = action.entity,
                        photos = photos,
                    )
                }

                is PullAction.Delete -> incidents.remove(action.id)
            }
        }
        this.meta = meta
    }

    override suspend fun resetForNewDatabase(databaseId: String) {
        resets += databaseId
        meta = SyncMetaEntity(databaseId = databaseId)
    }

    override suspend fun raiseAnnouncedChangeSeq(changeSeq: Long) {
        meta = meta.copy(announcedChangeSeq = maxOf(meta.announcedChangeSeq, changeSeq))
    }

    override suspend fun pruneOutOfScope(
        userId: String,
        updatedBefore: Instant,
    ) {
        prunes += userId to updatedBefore
    }

    override suspend fun getPendingIncidents(now: Instant): List<IncidentWithPhotos> = incidents.values
        .filter { it.incident.isDue(now = now) }
        .sortedBy { it.incident.localUpdatedAt }

    override suspend fun markPhotoUploaded(
        photoId: String,
        remoteUrl: String,
    ) {
        incidents.replaceAll { _, local ->
            local.copy(
                photos =
                    local.photos.map { photo ->
                        if (photo.id == photoId) {
                            photo.copy(
                                remoteUrl = remoteUrl,
                                uploadState = PhotoUploadState.UPLOADED,
                            )
                        } else {
                            photo
                        }
                    },
            )
        }
    }

    override suspend fun commitPush(
        id: String,
        merge: (IncidentWithPhotos) -> PushCommit,
    ) {
        val current = incidents[id] ?: return
        val commit = merge(current)
        incidents[id] =
            IncidentWithPhotos(
                incident = commit.entity,
                photos = commit.replacementPhotos ?: current.photos,
            )
    }

    override suspend fun removePushed(
        id: String,
        basedOnRevision: Long,
    ) {
        if (incidents[id]?.incident?.localRevision == basedOnRevision) incidents.remove(id)
    }

    override suspend fun markNeedsAttention(
        id: String,
        reason: ConflictReason,
        error: String,
        basedOnRevision: Long,
    ) {
        val current = incidents[id] ?: return
        if (current.incident.localRevision != basedOnRevision) return
        incidents[id] =
            current.copy(
                incident =
                    current.incident.copy(
                        syncState = SyncState.NEEDS_ATTENTION,
                        conflictReason = reason,
                        lastSyncError = error,
                    ),
            )
    }

    override suspend fun recordFailure(
        id: String,
        error: String,
    ) {
        failures += id to error
        val current = incidents[id] ?: return
        incidents[id] =
            current.copy(
                incident =
                    current.incident.copy(
                        syncAttempts = current.incident.syncAttempts + 1,
                        lastSyncError = error,
                    ),
            )
    }

    private fun IncidentEntity.isDue(now: Instant): Boolean = when (syncState) {
        SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE -> true
        SyncState.PENDING_DELETE -> undoDeadline == null || undoDeadline <= now
        else -> false
    }
}
