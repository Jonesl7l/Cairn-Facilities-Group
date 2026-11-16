package com.jonesl7l.carinfg.core.domain.model.incident

data class IncidentLocation(
    val siteId: String? = null,
    val buildingId: String? = null,
    val floorId: String? = null,
    val roomId: String? = null,
    /** free text when the building isnt listed */
    val unlistedLocation: String? = null,
    val exactSpot: String? = null,
    val assetId: String? = null,
    val equipmentOutOfService: Boolean = false,
)
