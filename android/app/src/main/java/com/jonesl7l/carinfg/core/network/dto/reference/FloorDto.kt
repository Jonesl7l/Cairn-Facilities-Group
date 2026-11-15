package com.jonesl7l.carinfg.core.network.dto.reference

import kotlinx.serialization.Serializable

@Serializable
data class FloorDto(
    val id: String,
    val buildingId: String,
    val name: String,
)
