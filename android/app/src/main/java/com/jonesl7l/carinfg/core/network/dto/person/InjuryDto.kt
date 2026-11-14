package com.jonesl7l.carinfg.core.network.dto.person

import kotlinx.serialization.Serializable

@Serializable
data class InjuryDto(
    val nature: String,
    val bodyPart: String,
    val treatment: String,
    val notes: String? = null,
)
