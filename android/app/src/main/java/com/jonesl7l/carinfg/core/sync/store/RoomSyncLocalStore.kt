package com.jonesl7l.carinfg.core.sync.store

import androidx.room3.withWriteTransaction
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.dao.IncidentDao
import com.jonesl7l.carinfg.core.database.dao.SyncMetaDao
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import com.jonesl7l.carinfg.core.sync.pull.PullRuleHelper
import com.jonesl7l.carinfg.core.sync.push.PushCommit
import javax.inject.Inject
import kotlin.time.Instant
import timber.log.Timber

class RoomSyncLocalStore @Inject constructor(
    private val database: AppDatabase,
    private val incidentDao: IncidentDao,
    private val syncMetaDao: SyncMetaDao,
) : SyncLocalStore {
    override suspend fun getSyncMeta(): SyncMetaEntity = syncMetaDao.get() ?: SyncMetaEntity.initial

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
            syncMetaDao.upsert(meta = SyncMetaEntity.initial.copy(databaseId = databaseId))
        }
    }

    override suspend fun pruneOutOfScope(
        userId: String,
        updatedBefore: Instant,
    ) {
        incidentDao.pruneOutOfScope(
            userId = userId,
            closedStatuses = PullRuleHelper.closedStatuses.toList(),
            updatedBefore = updatedBefore,
        )
    }

    override suspend fun getPendingIncidents(now: Instant): List<IncidentWithPhotos> =
        incidentDao.getPendingIncidents(now = now)

    override suspend fun markPhotoUploaded(
        photoId: String,
        remoteUrl: String,
    ) {
        incidentDao.markPhotoUploaded(photoId = photoId, remoteUrl = remoteUrl)
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
        val removed = incidentDao.deleteIfUnchanged(id = id, basedOnRevision = basedOnRevision)
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
        incidentDao.recordSyncFailure(id = id, error = error)
    }

    private suspend fun apply(action: PullAction) {
        when (action) {
            PullAction.Skip -> Unit

            is PullAction.Upsert -> if (isUnchanged(id = action.entity.id, basedOnRevision = action.basedOnRevision)) {
                incidentDao.upsertIncident(incident = action.entity)
                action.replacementPhotos?.let { photos ->
                    incidentDao.deletePhotos(incidentId = action.entity.id)
                    incidentDao.upsertPhotos(photos = photos)
                }
            }

            is PullAction.Delete -> if (isUnchanged(id = action.id, basedOnRevision = action.basedOnRevision)) {
                incidentDao.deleteIncident(id = action.id)
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
}
