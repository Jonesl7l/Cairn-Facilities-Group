package com.jonesl7l.carinfg.core.domain.model.photo

import kotlin.time.Instant

data class Photo(
    val id: String,
    val incidentId: String,
    val position: Int,
    val caption: String?,
    // local until uploaded, remote only when synced from elsewhere
    val localPath: String?,
    val remoteUrl: String?,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    val takenAt: Instant,
    val uploadState: PhotoUploadState,
)
