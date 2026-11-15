package com.jonesl7l.carinfg.core.network.dto.reference

import com.jonesl7l.carinfg.core.network.dto.user.UserDto
import kotlinx.serialization.Serializable

@Serializable
data class ReferenceDataDto(
    val sites: List<SiteDto>,
    val buildings: List<BuildingDto>,
    val floors: List<FloorDto>,
    val rooms: List<RoomDto>,
    val assets: List<AssetDto>,
    val technicians: List<UserDto>,
)
