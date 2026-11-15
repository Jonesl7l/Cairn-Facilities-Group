package com.jonesl7l.carinfg.core.database.entity.reference

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "buildings",
    indices = [Index(value = ["siteId"])],
)
data class BuildingEntity(
    @PrimaryKey val id: String,
    val siteId: String,
    val name: String,
)
