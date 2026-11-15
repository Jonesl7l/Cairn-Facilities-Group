package com.jonesl7l.carinfg.core.sync.push

import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity

data class PushCommit(
    val entity: IncidentEntity,
    val replacementPhotos: List<PhotoEntity>?,
)
