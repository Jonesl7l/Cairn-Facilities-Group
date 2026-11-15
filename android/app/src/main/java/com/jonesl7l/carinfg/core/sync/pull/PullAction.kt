package com.jonesl7l.carinfg.core.sync.pull

import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.database.entity.photo.PhotoEntity

sealed interface PullAction {
    val basedOnRevision: Long?

    data class Upsert(
        val entity: IncidentEntity,
        val replacementPhotos: List<PhotoEntity>?,
        override val basedOnRevision: Long?,
    ) : PullAction

    data class Delete(
        val id: String,
        override val basedOnRevision: Long?,
    ) : PullAction

    data object Skip : PullAction {
        override val basedOnRevision: Long? = null
    }
}
