package com.jonesl7l.carinfg.core.data.mapper

import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity
import com.jonesl7l.carinfg.core.database.entity.reference.BuildingEntity
import com.jonesl7l.carinfg.core.database.entity.reference.FloorEntity
import com.jonesl7l.carinfg.core.database.entity.reference.ReferenceEntities
import com.jonesl7l.carinfg.core.database.entity.reference.RoomEntity
import com.jonesl7l.carinfg.core.database.entity.reference.SiteEntity
import com.jonesl7l.carinfg.core.database.entity.reference.TechnicianEntity
import com.jonesl7l.carinfg.core.domain.model.user.User
import com.jonesl7l.carinfg.core.network.dto.reference.AssetDto
import com.jonesl7l.carinfg.core.network.dto.reference.BuildingDto
import com.jonesl7l.carinfg.core.network.dto.reference.FloorDto
import com.jonesl7l.carinfg.core.network.dto.reference.ReferenceDataDto
import com.jonesl7l.carinfg.core.network.dto.reference.RoomDto
import com.jonesl7l.carinfg.core.network.dto.reference.SiteDto
import com.jonesl7l.carinfg.core.network.dto.user.UserDto

// region ReferenceDataDto
fun ReferenceDataDto.toEntities(): ReferenceEntities = ReferenceEntities(
    sites = sites.map { it.toEntity() },
    buildings = buildings.map { it.toEntity() },
    floors = floors.map { it.toEntity() },
    rooms = rooms.map { it.toEntity() },
    assets = assets.map { it.toEntity() },
    technicians = technicians.map { it.toTechnicianEntity() },
)
// endregion

// region SiteDto
private fun SiteDto.toEntity() = SiteEntity(
    id = id,
    name = name,
)
// endregion

// region BuildingDto
private fun BuildingDto.toEntity() = BuildingEntity(
    id = id,
    siteId = siteId,
    name = name,
)
// endregion

// region FloorDto
private fun FloorDto.toEntity() = FloorEntity(
    id = id,
    buildingId = buildingId,
    name = name,
)
// endregion

// region RoomDto
private fun RoomDto.toEntity() = RoomEntity(
    id = id,
    floorId = floorId,
    name = name,
)
// endregion

// region AssetDto
private fun AssetDto.toEntity() = AssetEntity(
    id = id,
    roomId = roomId,
    name = name,
    code = code,
)
// endregion

// region UserDto
private fun UserDto.toTechnicianEntity() = TechnicianEntity(
    id = id,
    username = username,
    displayName = displayName,
)

fun UserDto.toDomain() = User(
    id = id,
    username = username,
    displayName = displayName,
    role = WireValueMapper.toUserRole(raw = role),
)
// endregion
