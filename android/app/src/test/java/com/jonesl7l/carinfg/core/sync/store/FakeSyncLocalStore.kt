package com.jonesl7l.carinfg.core.sync.store

import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import kotlin.time.Instant

class FakeSyncLocalStore : SyncLocalStore {
    var meta = SyncMetaEntity.initial
    val incidents = mutableMapOf<String, IncidentWithPhotos>()
    val resets = mutableListOf<String>()
    val prunes = mutableListOf<Pair<String, Instant>>()

    override suspend fun getSyncMeta(): SyncMetaEntity = meta

    override suspend fun getIncidents(ids: List<String>): Map<String, IncidentWithPhotos> =
        incidents.filterKeys { it in ids }

    override suspend fun applyPullPage(
        actions: List<PullAction>,
        meta: SyncMetaEntity,
    ) {
        actions.forEach { action ->
            when (action) {
                PullAction.Skip -> Unit

                is PullAction.Upsert -> {
                    val photos = action.replacementPhotos ?: incidents[action.entity.id]?.photos.orEmpty()
                    incidents[action.entity.id] = IncidentWithPhotos(incident = action.entity, photos = photos)
                }

                is PullAction.Delete -> incidents.remove(action.id)
            }
        }
        this.meta = meta
    }

    override suspend fun resetForNewDatabase(databaseId: String) {
        resets += databaseId
        meta = SyncMetaEntity.initial.copy(databaseId = databaseId)
    }

    override suspend fun pruneOutOfScope(
        userId: String,
        updatedBefore: Instant,
    ) {
        prunes += userId to updatedBefore
    }
}
