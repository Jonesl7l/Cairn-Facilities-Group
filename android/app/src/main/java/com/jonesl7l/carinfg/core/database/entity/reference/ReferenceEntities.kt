package com.jonesl7l.carinfg.core.database.entity.reference

data class ReferenceEntities(
    val sites: List<SiteEntity>,
    val buildings: List<BuildingEntity>,
    val floors: List<FloorEntity>,
    val rooms: List<RoomEntity>,
    val assets: List<AssetEntity>,
    val technicians: List<TechnicianEntity>,
)
