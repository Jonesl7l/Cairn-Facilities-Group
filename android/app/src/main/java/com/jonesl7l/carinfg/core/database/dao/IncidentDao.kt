package com.jonesl7l.carinfg.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentWithPhotos
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Upsert
    suspend fun upsertIncident(incident: IncidentEntity)

    @Upsert
    suspend fun upsertPhotos(photos: List<PhotoEntity>)

    @Transaction
    @Query("SELECT * FROM incidents WHERE id = :id")
    fun observeIncident(id: String): Flow<IncidentWithPhotos?>

    @Transaction
    @Query("SELECT * FROM incidents WHERE id = :id")
    suspend fun getIncident(id: String): IncidentWithPhotos?

    @Query("DELETE FROM incidents WHERE id = :id")
    suspend fun deleteIncident(id: String)
}
