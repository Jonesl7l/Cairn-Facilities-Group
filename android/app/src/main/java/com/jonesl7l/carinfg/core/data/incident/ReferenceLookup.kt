package com.jonesl7l.carinfg.core.data.incident

import com.jonesl7l.carinfg.core.database.entity.reference.AssetEntity

data class ReferenceLookup(
    val sites: Map<String, String> = emptyMap(),
    val buildings: Map<String, String> = emptyMap(),
    val floors: Map<String, String> = emptyMap(),
    val rooms: Map<String, String> = emptyMap(),
    val assets: Map<String, AssetEntity> = emptyMap(),
    val technicians: Map<String, String> = emptyMap(),
)
