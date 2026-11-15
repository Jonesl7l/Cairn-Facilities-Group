package com.jonesl7l.carinfg.core.network.dto.reference

import kotlinx.serialization.Serializable

@Serializable
data class AssetDto(
    val id: String,
    val roomId: String,
    val name: String,
    val code: String,
)
