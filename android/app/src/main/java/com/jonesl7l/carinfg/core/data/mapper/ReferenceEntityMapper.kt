package com.jonesl7l.carinfg.core.data.mapper

import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity
import com.jonesl7l.carinfg.core.network.dto.reference.AssetDto
import com.jonesl7l.carinfg.core.network.dto.reference.BuildingDto
import com.jonesl7l.carinfg.core.network.dto.reference.FloorDto
import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import com.jonesl7l.carinfg.core.network.dto.reference.RoomDto
import com.jonesl7l.carinfg.core.network.dto.reference.SiteDto
import com.jonesl7l.carinfg.core.network.dto.user.UserDto

object ReferenceEntityMapper {
    fun toEntities(dto: ReferenceDataDto): ReferenceEntities = ReferenceEntities(
        sites = dto.sites.map(::toEntity),
        buildings = dto.buildings.map(::toEntity),
        floors = dto.floors.map(::toEntity),
        rooms = dto.rooms.map(::toEntity),
        assets = dto.assets.map(::toEntity),
        technicians = dto.technicians.map(::toTechnicianEntity),
    )

    private fun toEntity(dto: SiteDto) = SiteEntity(
        id = dto.id,
        name = dto.name,
    )

    private fun toEntity(dto: BuildingDto) = BuildingEntity(
        id = dto.id,
        siteId = dto.siteId,
        name = dto.name,
    )

    private fun toEntity(dto: FloorDto) = FloorEntity(
        id = dto.id,
        buildingId = dto.buildingId,
        name = dto.name,
    )

    private fun toEntity(dto: RoomDto) = RoomEntity(
        id = dto.id,
        floorId = dto.floorId,
        name = dto.name,
    )

    private fun toEntity(dto: AssetDto) = AssetEntity(
        id = dto.id,
        roomId = dto.roomId,
        name = dto.name,
        code = dto.code,
    )

    private fun toTechnicianEntity(dto: UserDto) = TechnicianEntity(
        id = dto.id,
        username = dto.username,
        displayName = dto.displayName,
    )
}
