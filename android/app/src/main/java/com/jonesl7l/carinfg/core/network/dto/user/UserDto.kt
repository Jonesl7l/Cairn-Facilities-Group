package com.jonesl7l.carinfg.core.network.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String,
    val username: String,
    val displayName: String,
    val role: String,
)
