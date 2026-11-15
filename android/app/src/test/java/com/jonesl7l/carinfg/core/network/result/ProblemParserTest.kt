package com.jonesl7l.carinfg.core.network.result

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jonesl7l.carinfg.core.network.ApiTestHelper.errorResponse
import com.jonesl7l.carinfg.core.network.ApiTestHelper.incidentDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.json
import com.jonesl7l.carinfg.core.network.ApiTestHelper.now
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemDetailsDto
import com.jonesl7l.carinfg.core.network.ApiTestHelper.problemJson
import com.jonesl7l.carinfg.core.network.dto.problem.ProblemFieldErrorDto
import com.jonesl7l.carinfg.core.network.http.HttpHeaders
import com.jonesl7l.carinfg.core.network.http.HttpStatus
import kotlin.time.Duration.Companion.seconds
import org.junit.Test

class ProblemParserTest {
    private val parser = ProblemParser(json = json)

    @Test
    fun `statuses without extra context map to their error`() {
        val cases = listOf(
            HttpStatus.BAD_REQUEST to ApiError.MalformedRequest,
            HttpStatus.UNAUTHORIZED to ApiError.Unauthenticated,
            HttpStatus.FORBIDDEN to ApiError.Forbidden,
            HttpStatus.NOT_FOUND to ApiError.NotFound,
            HttpStatus.PRECONDITION_FAILED to ApiError.VersionConflict(current = null),
            HttpStatus.PAYLOAD_TOO_LARGE to ApiError.PayloadTooLarge,
            HttpStatus.UNSUPPORTED_MEDIA_TYPE to ApiError.UnsupportedMediaType,
            HttpStatus.PRECONDITION_REQUIRED to ApiError.PreconditionRequired,
            HttpStatus.INTERNAL_SERVER_ERROR to ApiError.Server(status = HttpStatus.INTERNAL_SERVER_ERROR),
            UNLISTED_SERVER_ERROR to ApiError.Server(status = UNLISTED_SERVER_ERROR),
            UNLISTED_CLIENT_ERROR to ApiError.Unexpected(status = UNLISTED_CLIENT_ERROR),
        )
        cases.forEach { (status, expected) ->
            val response = errorResponse(
                status = status,
                body = problemJson(problem = problemDetailsDto(status = status)),
            )

            assertThat(parser.parse(response = response)).isEqualTo(expected)
        }
    }

    @Test
    fun `a missing or non problem body still maps by status`() {
        assertThat(parser.parse(response = errorResponse(status = HttpStatus.INTERNAL_SERVER_ERROR)))
            .isEqualTo(ApiError.Server(status = HttpStatus.INTERNAL_SERVER_ERROR))
        assertThat(parser.parse(response = errorResponse(status = HttpStatus.FORBIDDEN, body = "<html>no</html>")))
            .isEqualTo(ApiError.Forbidden)
    }

    @Test
    fun `401 with the invalid credentials code is a login failure`() {
        val problem = problemDetailsDto(status = HttpStatus.UNAUTHORIZED, code = "INVALID_CREDENTIALS")

        val response = errorResponse(status = HttpStatus.UNAUTHORIZED, body = problemJson(problem = problem))

        assertThat(parser.parse(response = response)).isEqualTo(ApiError.InvalidCredentials)
    }

    @Test
    fun `409 carries the current status`() {
        val problem = problemDetailsDto(status = HttpStatus.CONFLICT, currentStatus = "IN_PROGRESS")

        val response = errorResponse(status = HttpStatus.CONFLICT, body = problemJson(problem = problem))

        assertThat(parser.parse(response = response)).isEqualTo(ApiError.StatusLocked(currentStatus = "IN_PROGRESS"))
    }

    @Test
    fun `412 carries the current server copy`() {
        val current = incidentDto(version = 5)
        val problem =
            problemDetailsDto(
                status = HttpStatus.PRECONDITION_FAILED,
                current = current,
            )

        val response = errorResponse(status = HttpStatus.PRECONDITION_FAILED, body = problemJson(problem = problem))

        assertThat(parser.parse(response = response)).isEqualTo(ApiError.VersionConflict(current = current))
    }

    @Test
    fun `410 carries who deleted it`() {
        val problem = problemDetailsDto(
            status = HttpStatus.GONE,
            deletedAt = now,
            deletedBy = "operator",
            referenceNumber = "INC-2026-00001",
        )

        val response = errorResponse(status = HttpStatus.GONE, body = problemJson(problem = problem))
        val expected = ApiError.IncidentDeleted(
            deletedAt = now,
            deletedBy = "operator",
            referenceNumber = "INC-2026-00001",
        )

        assertThat(parser.parse(response = response)).isEqualTo(expected)
    }

    @Test
    fun `422 carries field errors, or the photo limit by code`() {
        val fieldErrors = listOf(ProblemFieldErrorDto(field = "title", code = "TOO_SHORT", message = "Too short"))
        val validation = problemDetailsDto(status = HttpStatus.UNPROCESSABLE_CONTENT, errors = fieldErrors)
        val photoLimit = problemDetailsDto(status = HttpStatus.UNPROCESSABLE_CONTENT, code = "PHOTO_LIMIT_EXCEEDED")

        val validationResponse = errorResponse(
            status = HttpStatus.UNPROCESSABLE_CONTENT,
            body = problemJson(problem = validation),
        )
        val photoLimitResponse = errorResponse(
            status = HttpStatus.UNPROCESSABLE_CONTENT,
            body = problemJson(problem = photoLimit),
        )

        assertThat(parser.parse(response = validationResponse))
            .isEqualTo(ApiError.ValidationFailed(fieldErrors = fieldErrors))
        assertThat(parser.parse(response = photoLimitResponse)).isEqualTo(ApiError.PhotoLimitExceeded)
    }

    @Test
    fun `503 carries retry after in seconds`() {
        val response =
            errorResponse(status = HttpStatus.SERVICE_UNAVAILABLE, headers = mapOf(HttpHeaders.RETRY_AFTER to "30"))

        assertThat(parser.parse(response = response)).isEqualTo(ApiError.ServiceUnavailable(retryAfter = 30.seconds))
    }

    @Test
    fun `503 with a date retry after leaves it to backoff`() {
        val response = errorResponse(
            status = HttpStatus.SERVICE_UNAVAILABLE,
            headers = mapOf(HttpHeaders.RETRY_AFTER to "Fri, 13 Nov 2026 12:00:00 GMT"),
        )

        assertThat(parser.parse(response = response)).isEqualTo(ApiError.ServiceUnavailable(retryAfter = null))
    }

    private companion object {
        const val UNLISTED_SERVER_ERROR = 502
        const val UNLISTED_CLIENT_ERROR = 418
    }
}
