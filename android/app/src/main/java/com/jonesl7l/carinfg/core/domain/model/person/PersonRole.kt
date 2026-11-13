package com.jonesl7l.carinfg.core.domain.model.person

sealed interface PersonRole {
    data object Injured : PersonRole

    data object Witness : PersonRole

    data object Involved : PersonRole

    data class Unknown(val raw: String) : PersonRole
}
