package com.jonesl7l.carinfg.core.domain.model.person

sealed interface PersonType {
    data object Employee : PersonType

    data object Contractor : PersonType

    data object Visitor : PersonType

    data object MemberOfPublic : PersonType

    data class Unknown(val raw: String) : PersonType
}
