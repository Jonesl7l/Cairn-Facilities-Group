package com.jonesl7l.carinfg.core.domain.model.person

data class Person(
    val id: String,
    val name: String,
    val type: PersonType,
    val role: PersonRole,
    val department: String?,
    val position: String?,
    val company: String?,
    val contactNumber: String?,
    val injury: InjuryDetails?,
)
