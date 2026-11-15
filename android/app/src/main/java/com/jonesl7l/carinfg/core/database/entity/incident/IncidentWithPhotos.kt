package com.jonesl7l.carinfg.core.database.entity.incident

import androidx.room3.Embedded
import androidx.room3.Relation
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity

data class IncidentWithPhotos(
    @Embedded val incident: IncidentEntity,
    @Relation(
        parentColumns = ["id"],
        entityColumns = ["incidentId"],
    )
    val photos: List<PhotoEntity>,
)
