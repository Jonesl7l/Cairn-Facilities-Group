package com.jonesl7l.carinfg.core.sync.store

import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.sync.SyncMetaEntity
import com.jonesl7l.carinfg.core.sync.pull.PullAction
import kotlin.time.Instant

interface SyncLocalStore {
    suspend fun getSyncMeta(): SyncMetaEntity

    suspend fun getIncidents(ids: List<String>): Map<String, IncidentWithPhotos>

    suspend fun applyPullPage(
        actions: List<PullAction>,
        meta: SyncMetaEntity,
    )

    suspend fun resetForNewDatabase(databaseId: String)

    suspend fun pruneOutOfScope(
        userId: String,
        updatedBefore: Instant,
    )
}
