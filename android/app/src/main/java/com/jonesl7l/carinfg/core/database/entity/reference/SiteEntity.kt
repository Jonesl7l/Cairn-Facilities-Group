package com.jonesl7l.carinfg.core.database.entity.reference

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "sites")
data class SiteEntity(
    @PrimaryKey val id: String,
    val name: String,
)
