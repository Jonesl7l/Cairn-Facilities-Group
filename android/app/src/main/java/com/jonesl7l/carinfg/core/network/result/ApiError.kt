package com.jonesl7l.carinfg.core.network.result

import com.jonesl7l.carinfg.core.network.dto.incident.IncidentDto
import com.jonesl7l.carinfg.core.network.dto.problem.ProblemFieldErrorDto
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Instant

sealed interface ApiError {
    val category: ErrorCategory

    data object Unauthenticated : ApiError {
        override val category = ErrorCategory.SESSION
    }

    data object InvalidCredentials : ApiError {
        override val category = ErrorCategory.NEEDS_USER
    }

    /** The UI hides disallowed actions, so a 403 is a bug. */
    data object Forbidden : ApiError {
        override val category = ErrorCategory.BUG
    }

    data object MalformedRequest : ApiError {
        override val category = ErrorCategory.BUG
    }

    data class ValidationFailed(val fieldErrors: List<ProblemFieldErrorDto>) : ApiError {
        override val category = ErrorCategory.NEEDS_USER
    }

    data object PhotoLimitExceeded : ApiError {
        override val category = ErrorCategory.NEEDS_USER
    }

    data object NotFound : ApiError {
        override val category = ErrorCategory.NEEDS_USER
    }

    data class IncidentDeleted(
        val deletedAt: Instant?,
        val deletedBy: String?,
        val referenceNumber: String?,
    ) : ApiError {
        override val category = ErrorCategory.NEEDS_USER
    }

    data class VersionConflict(val current: IncidentDto?) : ApiError {
        override val category = ErrorCategory.RETRY
    }

    data object PreconditionRequired : ApiError {
        override val category = ErrorCategory.BUG
    }

    data class StatusLocked(val currentStatus: String?) : ApiError {
        override val category = ErrorCategory.NEEDS_USER
    }

    data object PayloadTooLarge : ApiError {
        override val category = ErrorCategory.NEEDS_USER
    }

    data object UnsupportedMediaType : ApiError {
        override val category = ErrorCategory.BUG
    }

    data class ServiceUnavailable(val retryAfter: Duration?) : ApiError {
        override val category = ErrorCategory.RETRY
    }

    data class Server(val status: Int) : ApiError {
        override val category = ErrorCategory.RETRY
    }

    data class Network(val cause: IOException) : ApiError {
        override val category = ErrorCategory.RETRY
    }

    data class Unexpected(val status: Int?) : ApiError {
        override val category = ErrorCategory.BUG
    }
}
