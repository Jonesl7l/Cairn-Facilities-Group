package com.jonesl7l.carinfg.core.database.entity.reference

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "floors",
    indices = [Index(value = ["buildingId"])],
)
data class FloorEntity(
    @PrimaryKey val id: String,
    val buildingId: String,
    val name: String,
)
