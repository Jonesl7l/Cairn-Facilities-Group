package com.jonesl7l.carinfg.core.sync.store

import androidx.room3.withWriteTransaction
import com.jonesl7l.carinfg.core.database.AppDatabase
import com.jonesl7l.carinfg.core.database.dao.IncidentDao
import com.jonesl7l.carinfg.core.database.dao.SyncMetaDao
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import com.jonesl7l.carinfg.core.sync.pull.PullRuleHelper
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

    private suspend fun isUnchanged(
        id: String,
        basedOnRevision: Long?,
    ): Boolean {
        val unchanged = incidentDao.getLocalRevision(id = id) == basedOnRevision
        if (!unchanged) Timber.i("isUnchanged: skipped pulled change for %s, edited locally since it was read", id)
        return unchanged
    }
}
