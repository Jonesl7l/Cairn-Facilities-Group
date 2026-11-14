package com.jonesl7l.carinfg.core.network.dto.problem

import kotlinx.serialization.Serializable

@Serializable
data class ProblemFieldErrorDto(
    val field: String,
    val code: String? = null,
    val message: String? = null,
)
