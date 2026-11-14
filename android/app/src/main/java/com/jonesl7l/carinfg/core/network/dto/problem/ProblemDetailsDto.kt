package com.jonesl7l.carinfg.core.network.dto.problem

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val code: String? = null,
    val deletedAt: Instant? = null,
    val deletedBy: String? = null,
    val referenceNumber: String? = null,
    val currentStatus: String? = null,
    val errors: List<ProblemFieldErrorDto> = emptyList(),
)
