package com.jonesl7l.carinfg.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import com.jonesl7l.carinfg.core.domain.model.sync.ConflictReason
import com.jonesl7l.carinfg.core.domain.model.sync.SyncState
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    // never REPLACE: it deletes the row first and the cascade removes the photos
    @Upsert
    suspend fun upsertIncident(incident: IncidentEntity)

    @Upsert
    suspend fun upsertPhotos(photos: List<PhotoEntity>)

    // the relation runs two queries; the transaction keeps incident and photos consistent
    @Transaction
    @Query("SELECT * FROM incidents WHERE id = :id")
    fun observeIncident(id: String): Flow<IncidentWithPhotos?>

    @Transaction
    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getIncident(id: String): IncidentWithPhotos?

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncident(id: String)

    @Query("SELECT COUNT(*) FROM incidents WHERE syncState != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("DELETE FROM incidents")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM incidents WHERE id IN (:ids)")
    suspend fun getIncidents(ids: List<String>): List<IncidentWithPhotos>

    @Query("SELECT localRevision FROM incidents WHERE id = :id")
    suspend fun getLocalRevision(id: String): Long?

    @Query("DELETE FROM photos WHERE incidentId = :incidentId")
    suspend fun deletePhotos(incidentId: String)

    @Query("DELETE FROM incidents WHERE syncState = 'SYNCED'")
    suspend fun deleteSynced()

    @Transaction
    @Query("UPDATE photos SET remoteUrl = :remoteUrl, uploadState = 'UPLOADED' WHERE id = :photoId")
    suspend fun markPhotoUploaded(
        photoId: String,
        remoteUrl: String,
    )

    @Query("DELETE FROM incidents WHERE id = :id AND localRevision = :basedOnRevision")
    suspend fun deleteIfUnchanged(
        id: String,
        basedOnRevision: Long,
    ): Int

    @Query(
        "UPDATE incidents SET syncState = 'NEEDS_ATTENTION', conflictReason = :reason, lastSyncError = :error " +
            "WHERE id = :id AND localRevision = :basedOnRevision",
    )
    suspend fun markNeedsAttentionIfUnchanged(
        id: String,
        reason: ConflictReason,
        error: String,
        basedOnRevision: Long,
    ): Int

    @Query("UPDATE incidents SET syncAttempts = syncAttempts + 1, lastSyncError = :error WHERE id = :id")
    suspend fun recordSyncFailure(
        id: String,
        error: String,
    )

    @Query("SELECT * FROM incidents")
    fun observeIncidents(): Flow<List<IncidentEntity>>

    @Query("SELECT syncState FROM incidents")
    fun observeSyncStates(): Flow<List<SyncState>>

    @Transaction
    @Query("SELECT * FROM incidents WHERE syncState IN (:states) ORDER BY localUpdatedAt")
    suspend fun getIncidentsInStates(states: List<SyncState>): List<IncidentWithPhotos>

    @Query("SELECT * FROM incidents WHERE syncState = 'SYNCED'")
    suspend fun getSynced(): List<IncidentEntity>

    @Query("DELETE FROM incidents WHERE id IN (:ids)")
    suspend fun deleteIncidents(ids: List<String>)

    @Query("SELECT id FROM incidents")
    suspend fun getIncidentIds(): List<String>

    @Query("SELECT * FROM photos WHERE position = 0")
    fun observeCoverPhotos(): Flow<List<PhotoEntity>>

    @Query("SELECT localPath FROM photos WHERE localPath IS NOT NULL")
    suspend fun getLocalPhotoPaths(): List<String>
}
