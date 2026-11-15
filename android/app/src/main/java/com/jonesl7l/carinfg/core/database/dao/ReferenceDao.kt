package com.jonesl7l.carinfg.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceDao {
    @Query("SELECT * FROM sites ORDER BY name")
    fun observeSites(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM buildings WHERE siteId = :siteId ORDER BY name")
    fun observeBuildings(siteId: String): Flow<List<BuildingEntity>>

    @Query("SELECT * FROM floors WHERE buildingId = :buildingId ORDER BY name")
    fun observeFloors(buildingId: String): Flow<List<FloorEntity>>

    @Query("SELECT * FROM rooms WHERE floorId = :floorId ORDER BY name")
    fun observeRooms(floorId: String): Flow<List<RoomEntity>>

    @Query("SELECT * FROM assets WHERE roomId = :roomId ORDER BY name")
    fun observeAssets(roomId: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM technicians ORDER BY displayName")
    fun observeTechnicians(): Flow<List<TechnicianEntity>>

    @Query("SELECT NOT EXISTS (SELECT 1 FROM sites)")
    suspend fun isEmpty(): Boolean

    @Transaction
    suspend fun replaceAll(entities: ReferenceEntities) {
        deleteAll()
        insertAll(entities = entities)
    }

    @Transaction
    suspend fun insertAllIfEmpty(entities: ReferenceEntities) {
        if (isEmpty()) insertAll(entities = entities)
    }

    private suspend fun deleteAll() {
        deleteSites()
        deleteBuildings()
        deleteFloors()
        deleteRooms()
        deleteAssets()
        deleteTechnicians()
    }

    private suspend fun insertAll(entities: ReferenceEntities) {
        insertSites(sites = entities.sites)
        insertBuildings(buildings = entities.buildings)
        insertFloors(floors = entities.floors)
        insertRooms(rooms = entities.rooms)
        insertAssets(assets = entities.assets)
        insertTechnicians(technicians = entities.technicians)
    }

    @Query("DELETE FROM sites")
    suspend fun deleteSites()

    @Query("DELETE FROM buildings")
    suspend fun deleteBuildings()

    @Query("DELETE FROM floors")
    suspend fun deleteFloors()

    @Query("DELETE FROM rooms")
    suspend fun deleteRooms()

    @Query("DELETE FROM assets")
    suspend fun deleteAssets()

    @Query("DELETE FROM technicians")
    suspend fun deleteTechnicians()

    @Insert
    suspend fun insertSites(sites: List<SiteEntity>)

    @Insert
    suspend fun insertBuildings(buildings: List<BuildingEntity>)

    @Insert
    suspend fun insertFloors(floors: List<FloorEntity>)

    @Insert
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Insert
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Insert
    suspend fun insertTechnicians(technicians: List<TechnicianEntity>)
}
