package com.jonesl7l.carinfg.core.network.dto.incident

import kotlinx.serialization.Serializable

@Serializable
data class GpsLocationDto(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Float,
)
