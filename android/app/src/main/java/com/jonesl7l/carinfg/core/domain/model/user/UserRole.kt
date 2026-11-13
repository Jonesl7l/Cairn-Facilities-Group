package com.jonesl7l.carinfg.core.domain.model.user

sealed interface UserRole {
    data object Technician : UserRole

    data object Operator : UserRole

    data class Unknown(val raw: String) : UserRole
}
