package com.jonesl7l.carinfg.core.database.entity.reference

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "rooms",
    indices = [Index(value = ["floorId"])],
)
data class RoomEntity(
    @PrimaryKey val id: String,
    val floorId: String,
    val name: String,
)
