package com.jonesl7l.carinfg.core.sync.store

import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.sync.SyncCounts
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import com.jonesl7l.carinfg.core.sync.push.PushCommit
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

interface SyncLocalStore {
    suspend fun getSyncMeta(): SyncMetaEntity

    fun observeSyncCounts(): Flow<SyncCounts>

    suspend fun getIncidents(ids: List<String>): Map<String, IncidentWithPhotos>

    suspend fun applyPullPage(
        actions: List<PullAction>,
        meta: SyncMetaEntity,
    )

    suspend fun resetForNewDatabase(databaseId: String)

    suspend fun raiseAnnouncedChangeSeq(changeSeq: Long)

    suspend fun pruneOutOfScope(
        userId: String,
        updatedBefore: Instant,
    )

    suspend fun getPendingIncidents(now: Instant): List<IncidentWithPhotos>

    suspend fun markPhotoUploaded(
        photoId: String,
        remoteUrl: String,
    )

    suspend fun commitPush(
        id: String,
        merge: (IncidentWithPhotos) -> PushCommit,
    )

    suspend fun removePushed(
        id: String,
        basedOnRevision: Long,
    )

    suspend fun markNeedsAttention(
        id: String,
        reason: ConflictReason,
        error: String,
        basedOnRevision: Long,
    )

    suspend fun recordFailure(
        id: String,
        error: String,
    )
}
