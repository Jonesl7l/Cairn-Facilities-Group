package com.jonesl7l.carinfg.core.database.entity.reference

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "assets",
    indices = [Index(value = ["roomId"])],
)
data class AssetEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val name: String,
    val code: String,
)
