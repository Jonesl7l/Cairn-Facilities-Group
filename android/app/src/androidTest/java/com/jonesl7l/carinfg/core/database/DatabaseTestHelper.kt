package com.jonesl7l.carinfg.core.database

import androidx.room3.Room
import androidx.test.core.app.ApplicationProvider
import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity

object DatabaseTestHelper {
    fun inMemoryDatabase(): AppDatabase = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
        .build()

    fun referenceEntities(
        sites: List<SiteEntity> = listOf(siteEntity()),
        buildings: List<BuildingEntity> = listOf(buildingEntity()),
        floors: List<FloorEntity> = listOf(floorEntity()),
        rooms: List<RoomEntity> = listOf(roomEntity()),
        assets: List<AssetEntity> = listOf(assetEntity()),
        technicians: List<TechnicianEntity> = listOf(technicianEntity()),
    ) = ReferenceEntities(
        sites = sites,
        buildings = buildings,
        floors = floors,
        rooms = rooms,
        assets = assets,
        technicians = technicians,
    )

    fun siteEntity(
        id: String = "site-1",
        name: String = "Head Office",
    ) = SiteEntity(
        id = id,
        name = name,
    )

    fun buildingEntity(
        id: String = "building-1",
        siteId: String = "site-1",
        name: String = "North Block",
    ) = BuildingEntity(
        id = id,
        siteId = siteId,
        name = name,
    )

    fun floorEntity(
        id: String = "floor-1",
        buildingId: String = "building-1",
        name: String = "Ground Floor",
    ) = FloorEntity(
        id = id,
        buildingId = buildingId,
        name = name,
    )

    fun roomEntity(
        id: String = "room-1",
        floorId: String = "floor-1",
        name: String = "Lobby",
    ) = RoomEntity(
        id = id,
        floorId = floorId,
        name = name,
    )

    fun assetEntity(
        id: String = "asset-1",
        roomId: String = "room-1",
        name: String = "Revolving Door",
        code: String = "DOOR-001",
    ) = AssetEntity(
        id = id,
        roomId = roomId,
        name = name,
        code = code,
    )

    fun technicianEntity(
        id: String = "u1",
        username: String = "tech1",
        displayName: String = "Tech One",
    ) = TechnicianEntity(
        id = id,
        username = username,
        displayName = displayName,
    )
}
