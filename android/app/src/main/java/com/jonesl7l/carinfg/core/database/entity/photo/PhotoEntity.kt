package com.jonesl7l.carinfg.core.database.entity.photo

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.jonesl7l.carinfg.core.database.entity.incident.IncidentEntity
import com.jonesl7l.carinfg.core.domain.model.photo.PhotoUploadState

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = IncidentEntity::class,
            parentColumns = ["id"],
            childColumns = ["incidentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["incidentId"])],
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    val incidentId: String,
    val position: Int,
    val localPath: String?,
    val remoteUrl: String?,
    val uploadState: PhotoUploadState,
)
