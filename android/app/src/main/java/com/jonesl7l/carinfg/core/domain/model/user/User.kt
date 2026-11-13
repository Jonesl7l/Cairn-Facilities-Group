package com.jonesl7l.carinfg.core.domain.model.user

data class User(
    val id: String,
    val username: String,
    val displayName: String,
    val role: UserRole,
)
