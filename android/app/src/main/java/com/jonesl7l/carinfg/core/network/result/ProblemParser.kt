package com.jonesl7l.carinfg.core.network.result

import com.jonesl7l.carinfg.core.network.dto.problem.ProblemDetailsDto
import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import com.jonesl7l.carinfg.core.network.http.HttpStatus
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.Json
import retrofit2.Response

class ProblemParser @Inject constructor(private val json: Json) {
    fun parse(response: Response<*>): ApiError {
        val problem = decodeProblem(response = response)
        return when (val status = response.code()) {
            HttpStatus.BAD_REQUEST -> ApiError.MalformedRequest

            HttpStatus.UNAUTHORIZED -> if (problem?.code == INVALID_CREDENTIALS) {
                ApiError.InvalidCredentials
            } else {
                ApiError.Unauthenticated
            }

            HttpStatus.FORBIDDEN -> ApiError.Forbidden

            HttpStatus.NOT_FOUND -> ApiError.NotFound

            HttpStatus.CONFLICT -> ApiError.StatusLocked(currentStatus = problem?.currentStatus)

            HttpStatus.GONE -> ApiError.IncidentDeleted(
                deletedAt = problem?.deletedAt,
                deletedBy = problem?.deletedBy,
                referenceNumber = problem?.referenceNumber,
            )

            HttpStatus.PRECONDITION_FAILED -> ApiError.VersionConflict

            HttpStatus.PAYLOAD_TOO_LARGE -> ApiError.PayloadTooLarge

            HttpStatus.UNSUPPORTED_MEDIA_TYPE -> ApiError.UnsupportedMediaType

            HttpStatus.UNPROCESSABLE_CONTENT -> if (problem?.code == PHOTO_LIMIT_EXCEEDED) {
                ApiError.PhotoLimitExceeded
            } else {
                ApiError.ValidationFailed(fieldErrors = problem?.errors.orEmpty())
            }

            HttpStatus.PRECONDITION_REQUIRED -> ApiError.PreconditionRequired

            HttpStatus.SERVICE_UNAVAILABLE -> ApiError.ServiceUnavailable(
                // date values fall back to backoff
                retryAfter = response.headers()[HttpHeaders.RETRY_AFTER]?.toLongOrNull()?.seconds,
            )

            in HttpStatus.serverErrors -> ApiError.Server(status = status)

            else -> ApiError.Unexpected(status = status)
        }
    }

    private fun decodeProblem(response: Response<*>): ProblemDetailsDto? {
        val body = response.errorBody()?.string()
        if (body.isNullOrBlank()) return null
        return try {
            json.decodeFromString<ProblemDetailsDto>(body)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
        const val PHOTO_LIMIT_EXCEEDED = "PHOTO_LIMIT_EXCEEDED"
    }
}
