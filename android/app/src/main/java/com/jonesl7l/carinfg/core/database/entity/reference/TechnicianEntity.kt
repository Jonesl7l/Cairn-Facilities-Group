package com.jonesl7l.carinfg.core.database.entity.reference

import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "technicians")
data class TechnicianEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
)
