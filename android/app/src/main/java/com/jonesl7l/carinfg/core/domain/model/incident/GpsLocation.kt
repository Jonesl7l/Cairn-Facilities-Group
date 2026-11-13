package com.jonesl7l.carinfg.core.domain.model.incident

data class GpsLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)
