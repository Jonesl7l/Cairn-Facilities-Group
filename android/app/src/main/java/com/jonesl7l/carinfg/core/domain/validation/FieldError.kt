package com.jonesl7l.carinfg.core.domain.validation

data class FieldError(
    val field: IncidentField,
    val reason: ValidationReason,
)
