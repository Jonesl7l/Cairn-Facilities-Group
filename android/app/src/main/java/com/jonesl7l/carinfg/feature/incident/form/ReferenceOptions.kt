package com.jonesl7l.carinfg.feature.incident.form

import com.jonesl7l.carinfg.core.domain.model.reference.Asset
import com.jonesl7l.carinfg.core.domain.model.reference.Building
import com.jonesl7l.carinfg.core.domain.model.reference.Floor
import com.jonesl7l.carinfg.core.domain.model.reference.Room
import com.jonesl7l.carinfg.core.domain.model.reference.Site

data class ReferenceOptions(
    val sites: List<Site> = emptyList(),
    val buildings: List<Building> = emptyList(),
    val floors: List<Floor> = emptyList(),
    val rooms: List<Room> = emptyList(),
    val assets: List<Asset> = emptyList(),
)
