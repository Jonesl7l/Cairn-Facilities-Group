package com.jonesl7l.carinfg.core.sync.store

import androidx.room3.withWriteTransaction
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.dao.IncidentDao
import com.jonesl7l.carinfg.core.database.dao.SyncMetaDao
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.sync.SyncCounts
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import com.jonesl7l.carinfg.core.sync.pull.PullRuleHelper
import com.jonesl7l.carinfg.core.sync.push.PushCommit
import javax.inject.Inject
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber

class RoomSyncLocalStore @Inject constructor(
    private val database: AppDatabase,
    private val incidentDao: IncidentDao,
    private val syncMetaDao: SyncMetaDao,
) : SyncLocalStore {
    override suspend fun getSyncMeta(): SyncMetaEntity = syncMetaDao.get() ?: SyncMetaEntity()

    override fun observeSyncCounts(): Flow<SyncCounts> = incidentDao
        .observeSyncStates()
        .map { states ->
            SyncCounts(
                pending = states.count { it in pendingStates },
                needsAttention = states.count { it == SyncState.NEEDS_ATTENTION },
            )
        }

    override suspend fun getIncidents(ids: List<String>): Map<String, IncidentWithPhotos> =
        incidentDao.getIncidents(ids = ids).associateBy { it.incident.id }

    override suspend fun applyPullPage(
        actions: List<PullAction>,
        meta: SyncMetaEntity,
    ) {
        database.withWriteTransaction {
            actions.forEach { action -> apply(action = action) }
            syncMetaDao.upsert(meta = meta)
        }
    }

    override suspend fun resetForNewDatabase(databaseId: String) {
        database.withWriteTransaction {
            incidentDao.deleteSynced()
            syncMetaDao.upsert(meta = SyncMetaEntity(databaseId = databaseId))
        }
    }

    override suspend fun raiseAnnouncedChangeSeq(changeSeq: Long) {
        syncMetaDao.raiseAnnouncedChangeSeq(changeSeq = changeSeq)
    }

    override suspend fun pruneOutOfScope(
        userId: String,
        updatedBefore: Instant,
    ) {
        database.withWriteTransaction {
            val outOfScope =
                incidentDao.getSynced().filterNot {
                    PullRuleHelper.isInScope(
                        reporterId = it.reporterId,
                        status = it.status,
                        updatedAt = it.updatedAt,
                        userId = userId,
                        updatedBefore = updatedBefore,
                    )
                }
            if (outOfScope.isNotEmpty()) incidentDao.deleteIncidents(ids = outOfScope.map { it.id })
        }
    }

    override suspend fun getPendingIncidents(now: Instant): List<IncidentWithPhotos> = incidentDao
        .getIncidentsInStates(states = pendingStates.toList())
        .filter { it.incident.isDue(now = now) }

    override suspend fun markPhotoUploaded(
        photoId: String,
        remoteUrl: String,
    ) {
        incidentDao.markPhotoUploaded(
            photoId = photoId,
            remoteUrl = remoteUrl,
        )
    }

    override suspend fun commitPush(
        id: String,
        merge: (IncidentWithPhotos) -> PushCommit,
    ) {
        database.withWriteTransaction {
            val current = incidentDao.getIncident(id = id) ?: return@withWriteTransaction
            val commit = merge(current)
            incidentDao.upsertIncident(incident = commit.entity)
            commit.replacementPhotos?.let { photos ->
                incidentDao.deletePhotos(incidentId = id)
                incidentDao.upsertPhotos(photos = photos)
            }
        }
    }

    override suspend fun removePushed(
        id: String,
        basedOnRevision: Long,
    ) {
        val removed = incidentDao.deleteIfUnchanged(
            id = id,
            basedOnRevision = basedOnRevision,
        )
        if (removed == 0) Timber.i("removePushed: kept %s, edited locally since it was read", id)
    }

    override suspend fun markNeedsAttention(
        id: String,
        reason: ConflictReason,
        error: String,
        basedOnRevision: Long,
    ) {
        val marked =
            incidentDao.markNeedsAttentionIfUnchanged(
                id = id,
                reason = reason,
                error = error,
                basedOnRevision = basedOnRevision,
            )
        if (marked == 0) Timber.i("markNeedsAttention: kept %s pending, edited locally since it was read", id)
    }

    override suspend fun recordFailure(
        id: String,
        error: String,
    ) {
        incidentDao.recordSyncFailure(
            id = id,
            error = error,
        )
    }

    private suspend fun apply(action: PullAction) {
        when (action) {
            PullAction.Skip -> Unit

            is PullAction.Upsert -> {
                val unchanged =
                    isUnchanged(
                        id = action.entity.id,
                        basedOnRevision = action.basedOnRevision,
                    )
                if (!unchanged) return
                incidentDao.upsertIncident(incident = action.entity)
                action.replacementPhotos?.let { photos ->
                    incidentDao.deletePhotos(incidentId = action.entity.id)
                    incidentDao.upsertPhotos(photos = photos)
                }
            }

            is PullAction.Delete -> {
                val unchanged =
                    isUnchanged(
                        id = action.id,
                        basedOnRevision = action.basedOnRevision,
                    )
                if (unchanged) incidentDao.deleteIncident(id = action.id)
            }
        }
    }

    // the cursor still moves on; push's 412 retry reconciles a skipped change
    private suspend fun isUnchanged(
        id: String,
        basedOnRevision: Long?,
    ): Boolean {
        val unchanged = incidentDao.getLocalRevision(id = id) == basedOnRevision
        if (!unchanged) Timber.i("isUnchanged: skipped pulled change for %s, edited locally since it was read", id)
        return unchanged
    }

    private fun IncidentEntity.isDue(now: Instant): Boolean = syncState != SyncState.PENDING_DELETE || undoDeadline == null || undoDeadline <= now

    private companion object {
        val pendingStates = setOf(SyncState.PENDING_CREATE, SyncState.PENDING_UPDATE, SyncState.PENDING_DELETE)
    }
}
