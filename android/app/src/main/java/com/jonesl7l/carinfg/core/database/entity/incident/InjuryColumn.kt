package com.jonesl7l.carinfg.core.database.entity.incident

import kotlinx.serialization.Serializable

@Serializable
data class InjuryColumn(
    val nature: String,
    val bodyPart: String,
    val treatment: String?,
    val notes: String? = null,
)
