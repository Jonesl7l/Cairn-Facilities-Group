package com.jonesl7l.carinfg.core.network.dto.reference

import kotlinx.serialization.Serializable

@Serializable
data class BuildingDto(
    val id: String,
    val siteId: String,
    val name: String,
)
