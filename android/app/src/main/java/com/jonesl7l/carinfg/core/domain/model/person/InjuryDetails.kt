package com.jonesl7l.carinfg.core.domain.model.person

data class InjuryDetails(
    val nature: String,
    val bodyPart: String,
    val treatment: Treatment?,
    val notes: String?,
)
