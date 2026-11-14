package com.jonesl7l.carinfg.core.domain.model.photo

data class Photo(
    val id: String,
    val incidentId: String,
    val position: Int,
    // local until uploaded, remote only when synced from elsewhere
    val localPath: String?,
    val remoteUrl: String?,
    val uploadState: PhotoUploadState,
)
