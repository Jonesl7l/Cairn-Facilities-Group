package com.jonesl7l.carinfg.core.network.dto.person

import kotlinx.serialization.Serializable

@Serializable
data class PersonDto(
    val id: String,
    val name: String,
    val personType: String,
    val role: String,
    val department: String? = null,
    val position: String? = null,
    val company: String? = null,
    val contactNumber: String? = null,
    val injury: InjuryDto? = null,
)
