package com.jonesl7l.carinfg.core.network.dto.reference

import kotlinx.serialization.Serializable

@Serializable
data class RoomDto(
    val id: String,
    val floorId: String,
    val name: String,
)
