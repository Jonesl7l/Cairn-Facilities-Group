package com.jonesl7l.carinfg.core.domain.model.incident

data class IncidentLocation(
    val siteId: String?,
    val buildingId: String?,
    val floorId: String?,
    val roomId: String?,
    /** free text when the building isnt listed */
    val unlistedLocation: String?,
    val exactSpot: String?,
    val assetId: String?,
    val equipmentOutOfService: Boolean,
)
