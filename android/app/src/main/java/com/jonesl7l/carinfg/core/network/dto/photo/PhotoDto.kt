package com.jonesl7l.carinfg.core.network.dto.photo

import kotlinx.serialization.Serializable

@Serializable
data class PhotoDto(
    val id: String,
    val url: String,
)
