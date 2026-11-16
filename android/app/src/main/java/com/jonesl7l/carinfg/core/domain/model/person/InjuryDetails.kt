package com.jonesl7l.carinfg.core.domain.model.person

data class InjuryDetails(
    val nature: String = "",
    val bodyPart: String = "",
    // nullable so drafts can save incomplete
    val treatment: Treatment? = null,
    val notes: String? = null,
)
