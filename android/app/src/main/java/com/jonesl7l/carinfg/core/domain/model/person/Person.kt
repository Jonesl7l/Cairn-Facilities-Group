package com.jonesl7l.carinfg.core.domain.model.person

data class Person(
    val id: String,
    val name: String = "",
    // nullable so drafts can save incomplete
    val type: PersonType? = null,
    val role: PersonRole? = null,
    val department: String? = null,
    val position: String? = null,
    val company: String? = null,
    val contactNumber: String? = null,
    val injury: InjuryDetails? = null,
)
