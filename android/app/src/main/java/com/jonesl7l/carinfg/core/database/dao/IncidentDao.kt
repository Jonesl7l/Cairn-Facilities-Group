package com.jonesl7l.carinfg.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import kotlin.time.Instant
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

    @Query(
        "DELETE FROM incidents WHERE syncState = 'SYNCED' AND reporterId != :userId " +
            "AND status IN (:closedStatuses) AND (updatedAt IS NULL OR updatedAt < :updatedBefore)",
    )
    suspend fun pruneOutOfScope(
        userId: String,
        closedStatuses: List<String>,
        updatedBefore: Instant,
    )
}
